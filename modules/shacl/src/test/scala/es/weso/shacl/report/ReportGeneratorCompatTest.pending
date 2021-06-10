package es.weso.shacl.report

import java.io._
import java.nio.file.{Path, Paths}

import com.typesafe.config.{Config => TConfig, _}
import es.weso.rdf._
import es.weso.rdf.jena.RDFAsJenaModel
import es.weso.rdf.nodes.{IRI, RDFNode}
import es.weso.rdf.parser.RDFParser
import es.weso.shacl.converter.RDF2Shacl
import es.weso.shacl.{Schema, Shacl, manifest}
import es.weso.shacl.manifest._
import es.weso.shacl.validator.Validator
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should._
import es.weso.shacl.manifest.{Manifest, ManifestAction, Result => ManifestResult, _}
// import cats._
// import cats.data._ 
import cats.implicits._
import cats.effect._

// import scala.util.{Either, Left, Right}

class ReportGeneratorCompatTest extends AnyFunSpec with Matchers with RDFParser {

  val conf: TConfig = ConfigFactory.load()
//  val manifestFile = new File(conf.getString("manifestFile"))
  val outFile = conf.getString("EarlReportFile")
//  val baseIRI: Option[String] = Some(Paths.get(manifestFile.getCanonicalPath()).normalize().toUri.toString)

  val shaclFolder    = conf.getString("shaclCore")
  val name           = "manifest.ttl"
  val shaclFolderPath = Paths.get(shaclFolder)
  val shaclFolderURI = Paths.get(shaclFolder).normalize.toUri.toString
  val absoluteIri    = IRI(shaclFolderURI)

  var numTests  = 0
  var numNodeShapeTests  = 0
  var numPassed = 0
  var numFailed = 0
  var numErrors = 0
  val report = Report.empty

  describe(s"Generate EARL") {
   describe(s"Should write report to $outFile") {
    describeManifest(IRI(name), shaclFolderPath)
    val earlModel = report.generateEARL
    earlModel.write(new FileOutputStream(outFile), "TURTLE")
   }
   describe("Show counters") {
     it(s"Shows counters") {
      info(s"#tests=$numTests, #passed=$numPassed, #failed=$numFailed, #errors=$numErrors")
     }
   }
  }

  def describeManifest(name: RDFNode, parentFolder: Path): Unit = name match {
    case iri: IRI => {
      val fileName = Paths.get(parentFolder.toUri.resolve(iri.uri)).toString
      val cmp: IO[Unit] = RDF2Manifest.read(fileName, "TURTLE", Some(fileName), false).flatMap(_.use(
        manifest => {
          val newParent      = Paths.get(parentFolder.toUri.resolve(iri.uri))
          processManifest(manifest, name.getLexicalForm, newParent) // , rdfReader)
        }
      ))
      cmp.unsafeRunSync
      
/*      .value.unsafeRunSync match {
        case Left(e) => {
            fail(s"Error reading manifestTest file:$e")
        }
        case Right(pair) => {
          val (m, rdfReader) = pair
          
        }
      } */
    }
    case _ => println(s"describeManifest: Unsupported non-IRI node $name")
  }

  def processManifest(m: Manifest, 
                      name: String, 
                      parentFolder: Path,
                      // rdfManifest: RDFBuilder
                      ): IO[Unit] = {
    for ((includeNode, manifest) <- m.includes) {
      describeManifest(includeNode, parentFolder)
    }
    val vs: List[IO[Unit]] = m.entries.map(processEntry(_, name, parentFolder) //, rdfManifest)
    )
    vs.sequence.map(_ => ())  
  }

  def processEntry(
    e: manifest.Entry, 
    name: String, 
    parentFolder: Path 
    // , rdfManifest: RDFBuilder
    ): IO[Unit] = {
    val testUri = (new java.net.URI("urn:x-shacl-test:/" + parentFolder.getFileName + "/" + e.node.getLexicalForm)).toString
    val r: IO[Unit] = getData(e.action,name,parentFolder //,rdfManifest
                             ).flatMap(_.use(rdf => for {
      schema <- getSchema(e.action, name, parentFolder // , rdfManifest
                         )
      _ <- IO(validate(schema, rdf, e.result, testUri))
    } yield ()))
    r
  }

/*  def getSchema(a: ManifestAction,
                   fileName: String,
                   parentFolder: Path,
                   manifestRdf: RDFBuilder
                  ): IO[Schema] = {
   for {
    schema <- getSchema(a, fileName, parentFolder, manifestRdf)
//    dataRdf <- getData(a, fileName, parentFolder, manifestRdf, schemaRdf)
   } yield {
     schema // , dataRdf)
   }
 } */

  def getData(action: ManifestAction, 
              fileName: String, 
              parentFolder: Path
              // , manifestRdf: RDFReader
              ): IO[Resource[IO,RDFReader]] = {
    action.data match {
      case None => RDFAsJenaModel.empty
      // case Some(iri) if iri.isEmpty => Resource.pure[IO,RDFReader](manifestRdf)
      case Some(iri) => {
        val dataFileName = Paths.get(parentFolder.toUri.resolve(iri.uri)).toFile
        val dataFormat  = action.dataFormat.getOrElse(Shacl.defaultFormat)
        RDFAsJenaModel.fromFile(dataFileName, dataFormat)
      }
    }
  }

  def getSchema(a: ManifestAction,
                fileName: String,
                parentFolder: Path,
                // manifestRdf: RDFBuilder
               ): IO[Schema] = {
    a.schema match {
      case None => {
        info(s"No data in manifestAction $a")
        IO(Schema.empty) 
      }
/*      case Some(iri) if iri.isEmpty => for {
        schema <- RDF2Shacl.getShacl(manifestRdf)
      } yield schema */
      case Some(iri) => {
        val schemaFile = Paths.get(parentFolder.toUri.resolve(iri.uri)).toFile
        val schemaFormat = a.dataFormat.getOrElse(Shacl.defaultFormat)
        RDFAsJenaModel.fromFile(schemaFile, schemaFormat).flatMap(_.use(schemaRdf => for {
          schema <- {
            RDF2Shacl.getShacl(schemaRdf)
          }
        } yield schema))
      }
    }
  }



    def validate(schema: Schema, 
                 rdf: RDFReader,
                 expectedResult: ManifestResult,
                 testUri: String
                ): Unit = {
      val validator = Validator(schema)
      val result = validator.validateAll(rdf).unsafeRunSync()
      numTests += 1
      expectedResult match {
        case ReportResult(rep) => {
          var isOk = true
          var numNodeShapes = 0
          var numNodeShapesPassed = 0
          rep.failingNodesShapes.foreach { case (node,shape) => {
            numNodeShapes += 1
            result.result.fold(vr => {
              numErrors += 1
              isOk = false
              },
              typing => {
              if (typing._1.getFailedValues(node).map(_.id) contains (shape)) {
                numNodeShapesPassed += 1
                } else {
                isOk = false
              }
            })
           }
          }
          val item = SingleTestReport(
            passed = isOk,
            name = name,
            uriTest = testUri,
            testType = "Validation",
            moreInfo = s"NodeShapes: ${numNodeShapes}\nPassed: ${numNodeShapesPassed} \n${result.show}")
          numPassed += (if (isOk) 1 else 0)
          numErrors += (if (isOk) 0 else 1)
          report.addTestReport(item)
        }
        case BooleanResult(b) =>
          if (result.isOK == b) {
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
        case _ => {
          fail(s"Unsupported manifestTest result $result")
        }
      }
    }
}