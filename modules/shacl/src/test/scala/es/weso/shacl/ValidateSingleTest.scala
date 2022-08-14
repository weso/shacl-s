package es.weso.shacl

import cats.implicits._
import com.typesafe.config.{Config, ConfigFactory}
import es.weso.rdf.jena.RDFAsJenaModel
import es.weso.shacl.converter.RDF2Shacl
import es.weso.shacl.validator.Validator
import es.weso.utils.FileUtils._
import munit._
import scala.io.Source
import scala.util._
// import cats.data.EitherT
import cats.effect._
import es.weso.utils.IOUtils2.either2io

class ValidateSingleTest extends CatsEffectSuite {

  val name = "good7"

  val conf: Config = ConfigFactory.load()
  val shaclFolder: String = conf.getString("shaclTests")

  lazy val ignoreFiles: List[String] = List()

  test(s"Validate single $name, folder: $shaclFolder") {
    val file = getFileFromFolderWithExt(shaclFolder, name, "ttl").unsafeRunSync
    val str = Source.fromFile(file)("UTF-8").mkString
    validate(name, str)
  }

  def validate(name: String, str: String): Unit = {
    val cmp = for {
     res1 <- RDFAsJenaModel.fromString(str, "TURTLE")
     res2 <- RDFAsJenaModel.empty
     vv <- (res1, res2).tupled.use{ case (rdf,builder) => for {
      schema <- RDF2Shacl.getShacl(rdf)
      eitherresult <- Validator.validate(schema, rdf)
      result <- either2io(eitherresult)
      (typing,ok) = result
      report <- typing.toValidationReport.toRDF(builder)
      strReport <- report.serialize("TURTLE")
      _ <- if (!ok) IO.println(s"Not valid, report:\n$strReport")
           else ().pure[IO]  
    } yield ok}  
    } yield vv 
    assertIO(cmp, true)
  }

}
