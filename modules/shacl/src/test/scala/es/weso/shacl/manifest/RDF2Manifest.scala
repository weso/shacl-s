package es.weso.shacl.manifest

import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf._
import es.weso.rdf.jena.RDFAsJenaModel
import es.weso.rdf.nodes._
import es.weso.rdf.parser.RDFParser
// import es.weso.utils.FileUtils._
import ManifestPrefixes._
import es.weso.utils.EitherUtils._
import es.weso.rdf.parser._
// import cats.data.EitherT
import cats.effect._
import scala.util._
// import cats.data._
import cats.implicits._
// import es.weso.utils.IOUtils._
// import es.weso.utils.internal.CollectionCompat._
import fs2.Stream
import cats.arrow.FunctionK
import scala.concurrent.ExecutionContext
import fs2.Pipe
import java.nio.file.Paths

// case class RDF2ManifestException(v: String) extends RuntimeException(v)

case class RDF2Manifest(base: Option[IRI],
                        derefIncludes: Boolean) extends RDFParser with LazyLogging {


  def transf: FunctionK[IO, RDFParser] = new FunctionK[IO,RDFParser] {
    def apply[A](io: IO[A]): RDFParser[A] = liftIO(io)
  }
  def cnvResource[A](r: Resource[IO,A]): Resource[RDFParser,A] = r.mapK(transf)
                          
  def fromEitherS[A](e: Either[String,A]): RDFParser[A] = {
    fromEither(e.leftMap(RDF2ManifestException))
  }

  def rdf2Manifest(rdf: RDFReader, visited: List[RDFNode] = List()): RDFParser[Manifest] =
    for {
      mfs <- rdf2Manifests(rdf,visited)
      mf <- fromEitherS(takeSingle(mfs,"Number of manifests != 1"))
      // parseNodes(candidates.toList, manifest(List()))(rdf)
    } yield mf

  def rdf2Manifests(rdf: RDFReader, visited: List[RDFNode] = List()): RDFParser[List[Manifest]] =
    for {
      candidates <- fromRDFStream(rdf.subjectsWithType(mf_Manifest))
      ns <- parseNodes(candidates.toList, manifest(List()))
    } yield ns

  def manifest(visited: List[IRI]): RDFParser[Manifest] =  
    for {
      maybeLabel   <- stringFromPredicateOptional(rdfs_label)
      maybeComment <- stringFromPredicateOptional(rdfs_comment)
      entries      <- entries
      includes     <- includes(visited)
    } yield {
      Manifest(label = maybeLabel, comment = maybeComment, entries = entries.toList, includes = includes)
    }

  def entries: RDFParser[List[Entry]] =
    parsePropertyList(mf_entries, entry)

  def getEntryType(node: RDFNode): Either[String, EntryType] = {
    node match {
      case `sht_Validate`            => Right(Validate)
      case `sht_ValidationFailure`   => Right(ValidationFailure)
      case `sht_MatchNodeShape`      => Right(MatchNodeShape)
      case `sht_WellFormedSchema`    => Right(WellFormedSchema)
      case `sht_NonWellFormedSchema` => Right(NonWellFormedSchema)
      case `sht_ConvertSchemaSyntax` => Right(ConvertSchemaSyntax)
      case _                         => Left("Unexpected entry type: " + node)
    }
  }

  def entry: RDFParser[Entry] = for {
      n <- getNode
      entryTypeUri <- rdfType
      entryType    <- fromEitherS(getEntryType(entryTypeUri))
      maybeName    <- stringFromPredicateOptional(mf_name)
      actionNode   <- objectFromPredicate(mf_action)
      action       <- withNode(actionNode, action) 
      resultNode   <- objectFromPredicate(mf_result)
      result       <- withNode(resultNode, result) 
      statusIri    <- iriFromPredicate(mf_status)
      specRef      <- optional(iriFromPredicate(sht_specRef))
    } yield
      Entry(node = n,
            entryType = entryType,
            name = maybeName,
            action = action,
            result = result,
            status = Status(statusIri),
            specRef = specRef)

  def iriDataFormat2str(iri: IRI): Either[String, String] = {
    iri match {
      case `sht_TURTLE` => Right("TURTLE")
      case _            => Left("Unexpected schema format: " + iri)
    }
  }

  def iriSchemaFormat2str(iri: IRI): Either[String, String] = {
    iri match {
      case `sht_SHACLC` => Right("SHACLC")
      case `sht_TURTLE` => Right("TURTLE")
      case _            => Left(s"Unexpected schema format: $iri")
    }
  }

  private def action: RDFParser[ManifestAction] = 
    for {
      data <- optional(iriFromPredicate(sht_dataGraph))
      schema <- iriFromPredicateOptional(sht_shapesGraph)
      dataFormatIri      <- optional(iriFromPredicate(sht_data_format))
      dataFormat         <- mapOptional(dataFormatIri, iriDataFormat2str)
      schemaFormatIRI    <- optional(iriFromPredicate(sht_schema_format))
      schemaFormat       <- mapOptional(schemaFormatIRI, iriSchemaFormat2str)
      schemaOutputFormat <- optional(iriFromPredicate(sht_schema_output_format))
      triggerMode        <- optional(iriFromPredicate(sht_triggerMode))
      node               <- optional(oneOfPredicates(Seq(sht_node, sht_focus)))
      shape              <- optional(iriFromPredicate(sht_shape))
    } yield
      ManifestAction(
        schema = schema,
        schemaFormat = schemaFormat,
        data = data,
        dataFormat = dataFormat,
        triggerMode = triggerMode,
        schemaOutputFormat = schemaOutputFormat,
        node = node,
        shape = shape
      )

  private def result: RDFParser[Result] = for { 
    n <- getNode
    v <- n match {
        case BooleanLiteral(b) => ok(BooleanResult(b))
        case iri: IRI => 
         for { 
           b <- noType
           v <- if (b) { 
                  val r: RDFParser[Result] = ok(IRIResult(iri)) 
                  r
                }
                else compoundResult
         } yield v 
        case bNode: BNode => compoundResult
        case _            => parseFail("Unexpected type of result " + n)
      }
   } yield v


  private def compoundResult: RDFParser[Result] = for {
    n <- getNode
    maybeType <- optional(iriFromPredicate(rdf_type))
    v <- maybeType match {
      case None => parseFail(s"compoundResult. No rdf:type for node: $n")
      case Some(`sh_ValidationReport`) => for {
        report <- validationReport
      } yield ReportResult(report)
      case Some(other) => parseFail(s"compoundResult. rdf:type for node $n should be ${`sh_ValidationReport`}")
    }
  } yield v
  
  private def validationReport: RDFParser[ValidationReport] =
    parsePropertyValues(sh_result, violationError).map(ValidationReport(_))

  private def violationError: RDFParser[ViolationError] =
    for {
        errorType   <- optional(iriFromPredicate(rdf_type))
        focusNode   <- optional(objectFromPredicate(sh_focusNode))
        path        <- optional(iriFromPredicate(sh_path))
        severity    <- optional(iriFromPredicate(sh_severity))
        scc         <- optional(iriFromPredicate(sh_sourceConstraintComponent))
        sourceShape <- optional(iriFromPredicate(sh_sourceShape))
        value       <- optional(objectFromPredicate(sh_value))
      } yield {
        ViolationError(errorType, focusNode, path, severity, scc, sourceShape, value)
    }

  private def noType: RDFParser[Boolean] = for {
    types <- objectsFromPredicate(rdf_type)
  } yield types.isEmpty

  private def includes(visited: List[RDFNode]): RDFParser[List[(RDFNode, Option[Manifest])]] = 
      for {
        includes <- objectsFromPredicate(mf_include)
        result <- {
          val ds: List[RDFParser[(IRI, Option[Manifest])]] =
            includes.toList.map(iri => derefInclude(iri, base, iri +: visited))
          ds.sequence
        }
      } yield result

  /* TODO: The following code doesn't take into account possible loops */
  private def derefInclude(node: RDFNode,
                           base: Option[IRI],
                           visited: List[RDFNode]): RDFParser[(IRI, Option[Manifest])] = node match {
    case iri: IRI =>
      if (derefIncludes) {
        val iriResolved = base.fold(iri)(base => base.resolve(iri))
        liftIO(RDFAsJenaModel.fromURI(iriResolved.getLexicalForm, "TURTLE", Some(iriResolved))).flatMap(res => 
        cnvResource(res).use(rdf => for {
          manifest <- RDF2Manifest(Some(iriResolved), true).rdf2Manifest(rdf, iri +: visited)
          //manifest <- if (mfs.size == 1) ok(mfs.head)
          // else parseFail(s"More than one manifests found: ${mfs} at iri $iri")
        } yield (iri, Some(manifest))))
      } else ok((iri, None))
    case _ => 
       parseFail(s"Trying to deref an include from node $node which is not an IRI")
  }

  private def parsePropertyValues[A](pred: IRI, parser: RDFParser[A]): RDFParser[Set[A]] =
    for {
        values  <- objectsFromPredicate(pred)
        results <- parseNodes(values.toList, parser)
      } yield results.toSet

  private def parsePropertyList[A](pred: IRI, parser: RDFParser[A]): RDFParser[List[A]] =
    for {
        ls <- rdfListForPredicateAllowingNone(pred)
        vs <- parseNodes(ls, parser)
    } yield vs

  private def mapOptional[A, B](optA: Option[A], fn: A => Either[String, B]): RDFParser[Option[B]] = {
    optA match {
      case None => ok(None)
      case Some(x) => {
        fromEitherS(fn(x).map(_.some))
      }
    }
  }

  def oneOfPredicates(predicates: Seq[IRI]): RDFParser[IRI] = {
    val ps = predicates.map(iriFromPredicate(_))
    oneOf(ps)
  }

  /**
   * Override this method to provide more info
   */
  override def objectFromPredicate(p: IRI): RDFParser[RDFNode] = 
    for {
      rdf <- getRDF
      n <- getNode
      ts <- fromRDFStream(rdf.triplesWithSubjectPredicate(n, p))
      r <- ts.size match {
        case 0 =>
          parseFail(
            s"objectFromPredicate: Not found triples with subject $n and predicate $p \nRDF: ${rdf.serialize("TURTLE")}")
        case 1 => parseOk(ts.head.obj)
        case _ => parseFail("objectFromPredicate: More than one value from predicate " + p + " on node " + n)
      }
    } yield r
}

object RDF2Manifest extends LazyLogging {

  def read(fileName: String,
           format: String,
           base: Option[String],
           derefIncludes: Boolean
          ): IO[Resource[IO,Manifest]] = {
    for {
      cs <- getContents(fileName)
      iriBase <- base match {
          case None => None.pure[IO]
          case Some(str) => IO.fromEither(IRI.fromString(str).leftMap(s => new RuntimeException(s))).map(Some(_))
      }
      resRdf <- RDFAsJenaModel.fromString(cs.toString, format, iriBase)
      manifest <- IO(resRdf.evalMap(rdf => fromRDF(rdf,iriBase,derefIncludes))) 
    } yield manifest 
  }

  def fromRDF(rdf: RDFReader, base: Option[IRI], derefIncludes: Boolean): IO[Manifest] = {
    val cfg = Config(IRI("http://internal/base"), rdf)
    val x = RDF2Manifest(base,derefIncludes).rdf2Manifest(rdf)
    // EitherT(x.value.run(cfg).map(_.leftMap(_.toString)))
    x.value.run(cfg).flatMap(e => e.fold(
      err => IO.raiseError(err),
      IO(_)
    ))
  }

  // TODO: Move to common tools 
  private def getContents(fileName: String): IO[CharSequence] = {
    val path = Paths.get(fileName)
    implicit val cs = IO.contextShift(ExecutionContext.global)
    val decoder: Pipe[IO,Byte,String] = fs2.text.utf8Decode
    Stream.resource(Blocker[IO]).flatMap(blocker =>
      fs2.io.file.readAll[IO](path, blocker,4096).through(decoder)
    ).compile.string
  }

}
