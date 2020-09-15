package es.weso.shacl.report

import java.io._
import java.nio.file.{Path, Paths}
import com.typesafe.config._
import es.weso.rdf.{RDFBuilder, RDFReader}
import es.weso.rdf.jena.RDFAsJenaModel
import es.weso.rdf.nodes.{IRI, RDFNode}
import es.weso.rdf.parser.RDFParser
import es.weso.shacl.converter.RDF2Shacl
import es.weso.shacl.{Schema, Shacl, manifest}
import es.weso.shacl.validator.Validator
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should._
import es.weso.shacl.manifest.{Manifest, ManifestAction, Result => ManifestResult, _}
import cats.data.EitherT
import cats.effect._
import es.weso.utils.IOUtils
// import scala.util.{Either, Left, Right}

class ReportGeneratorCompatTest extends AnyFunSpec with Matchers with RDFParser {

  private val conf = ConfigFactory.load()
  private val outFile = conf.getString("EarlReportFile")

  private val shaclFolder = conf.getString("shaclCore")
  private val name           = "manifest.ttl"
  private val shaclFolderPath: Path = Paths.get(shaclFolder)
  // private val shaclFolderURI = Paths.get(shaclFolder).normalize.toUri.toString
  // private val absoluteIri    = IRI(shaclFolderURI)

  var numTests  = 0
  var numNodeShapeTests  = 0
  var numPassed = 0
  var numFailed = 0
  var numErrors = 0
  val report: Report = Report.empty

  describe(s"Generate EARL") {
   it(s"Should write report to $outFile") {
    describeManifest(IRI(name), shaclFolderPath)
    val earlModel = report.generateEARL
    earlModel.write(new FileOutputStream(outFile), "TURTLE")
    pprint.log(s"#tests=$numTests, #passed=$numPassed, #failed=$numFailed, #errors=$numErrors")
   }
  }

  def describeManifest(name: RDFNode, parentFolder: Path): Unit = name match {
    case iri: IRI =>
      val pathName = parentFolder.toUri.resolve(iri.uri)
      val fileName = Paths.get(pathName).toString
      RDF2Manifest.read(fileName, "TURTLE", Some(pathName.toString), derefIncludes = false).value.unsafeRunSync match {
        case Left(e) =>
          fail(s"Error reading manifestTest file:$e")
        case Right(pair) =>
          val (m, rdfReader) = pair
          val newParent      = Paths.get(parentFolder.toUri.resolve(iri.uri))
          processManifest(m, name.getLexicalForm, newParent, rdfReader)
      }
    case _ => println(s"describeManifest: Unsupported non-IRI node $name")
  }
  def processManifest(m: Manifest, name: String, parentFolder: Path, rdfManifest: RDFBuilder): Unit = {
    for ((includeNode, manifest) <- m.includes) {
      describeManifest(includeNode, parentFolder)
    }
    for (e <- m.entries)
      processEntry(e, name, parentFolder, rdfManifest)
  }

  def processEntry(e: manifest.Entry, name: String, parentFolder: Path, rdfManifest: RDFBuilder): Unit = {
    getSchemaRdf(e.action, name, parentFolder, rdfManifest).value.unsafeRunSync match {
        case Left(f) =>
          fail(s"Error processing Entry: $e \n $f")
        case Right((schema, rdf)) =>
          val testUri = new java.net.URI("urn:x-shacl-test:/" + parentFolder.getFileName + "/" + e.node.getLexicalForm).toString
          validate(schema, rdf, e.result, testUri)
    }
  }

  def getSchemaRdf(a: ManifestAction,
                   fileName: String,
                   parentFolder: Path,
                   manifestRdf: RDFBuilder
                  ): EitherT[IO,String, (Schema,RDFReader)] = {
   for {
    pair <- getSchema(a, fileName, parentFolder, manifestRdf)
    (schema, schemaRdf) = pair
    dataRdf <- getData(a, fileName, parentFolder, manifestRdf, schemaRdf)
   } yield {
     (schema, dataRdf)
   }
 }

  def getData(a: ManifestAction, fileName: String, parentFolder: Path, manifestRdf: RDFReader, schemaRdf: RDFReader): EitherT[IO,String, RDFReader] =
  {
    a.data match {
      case None => EitherT.liftF[IO,String,RDFReader](RDFAsJenaModel.empty)
      case Some(iri) if iri.isEmpty => EitherT.pure[IO,String](manifestRdf)
      case Some(iri) =>
        val dataFileName = Paths.get(parentFolder.toUri.resolve(iri.uri)).toFile
        val dataFormat  = a.dataFormat.getOrElse(Shacl.defaultFormat)
        EitherT.liftF(RDFAsJenaModel.fromFile(dataFileName, dataFormat)) // TODO: We don't normalize BNodes
    }
  }

  def getSchema(a: ManifestAction,
                fileName: String,
                parentFolder: Path,
                manifestRdf: RDFBuilder
               ): EitherT[IO, String, (Schema, RDFReader)] = {
    a.schema match {
      case None =>
        info(s"No data in manifestAction $a")
        for {
         emptyRDF <- IOUtils.io2es(RDFAsJenaModel.empty)
        } yield (Schema.empty, emptyRDF)
      case Some(iri) if iri.isEmpty =>
        val r:IO[(Schema,RDFBuilder)] = for {
          schema <- RDF2Shacl.getShacl(manifestRdf)
        } yield (schema, manifestRdf)
        IOUtils.io2esf(r)
      case Some(iri) =>
        val schemaFile = Paths.get(parentFolder.toUri.resolve(iri.uri)).toFile
        val schemaFormat = a.dataFormat.getOrElse(Shacl.defaultFormat)
        val r = for {
          schemaRdf <- RDFAsJenaModel.fromFile(schemaFile, schemaFormat)
          schema <- {
            RDF2Shacl.getShacl(schemaRdf)
          }
        } yield (schema, schemaRdf)
        IOUtils.io2esf(r)
    }
  }

    def validate(schema: Schema, rdf: RDFReader,
                 expectedResult: ManifestResult,
                 testUri: String
                ): Unit = {
      val validator = Validator(schema)
      val result = validator.validateAll(rdf)
      numTests += 1
      expectedResult match {
        case ReportResult(rep) =>
          var isOk = true
          var numNodeShapes = 0
          var numNodeShapesPassed = 0
          rep.failingNodesShapes.foreach { case (node,shape) =>
            numNodeShapes += 1
            result.unsafeRunSync().result.fold(vr => {
              numErrors += 1
              isOk = false
              },
              fb = typing => {
                if (typing._1.getFailedValues(node).map(_.id) contains shape) {
                  numNodeShapesPassed += 1
                } else {
                  isOk = false
                }
              })
          }
          val item = SingleTestReport(
            passed = isOk,
            name = name,
            uriTest = testUri,
            testType = "Validation",
            moreInfo = s"NodeShapes: $numNodeShapes\nPassed: $numNodeShapesPassed \n")
          numPassed += (if (isOk) 1 else 0)
          numErrors += (if (isOk) 0 else 1)
          report.addTestReport(item)
        case BooleanResult(b) =>
          if (result.unsafeRunSync().isOK == b) {
            numTests += 1
            numPassed += 1
            val item = SingleTestReport(
              passed = true,
              name = name,
              uriTest = testUri,
              testType = "Validation",
              moreInfo = s"Expected $b and found it")
            report.addTestReport(item)
          }
          else {
            numTests += 1
            numFailed += 1
            val item = SingleTestReport(
              passed = false,
              name = name,
              uriTest = testUri,
              testType = "Validation",
              moreInfo = s"Expected $b and found ${!b}")
            report.addTestReport(item)
          }
        case _ =>
          fail(s"Unsupported manifestTest result $result")
      }
    }
}