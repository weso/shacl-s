package es.weso.shacl

import cats.implicits._
import com.typesafe.config.{Config, ConfigFactory}
import es.weso.rdf.jena.RDFAsJenaModel
import es.weso.shacl.converter.RDF2Shacl
import es.weso.shacl.validator.Validator
import es.weso.utils.FileUtils._
import org.scalatest._
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should._

import scala.io.Source
import scala.util._
import cats.implicits._
import cats.data.EitherT
import cats.effect._
import es.weso.utils.IOUtils2.either2io

class ValidateSingleTest extends AnyFunSpec with Matchers with TryValues with OptionValues
  with SchemaMatchers {

  val name = "good7"

  val conf: Config = ConfigFactory.load()
  val shaclFolder: String = conf.getString("shaclTests")

  lazy val ignoreFiles: List[String] = List()

  describe("Validate single") {
    val file = getFileFromFolderWithExt(shaclFolder, name, "ttl").unsafeRunSync
    it(s"Should validate file $name in folder $shaclFolder") {
      val str = Source.fromFile(file)("UTF-8").mkString
      validate(name, str)
    }
  }

  def validate(name: String, str: String): Unit = {
    val cmp = (
      RDFAsJenaModel.fromString(str, "TURTLE"), 
      RDFAsJenaModel.empty
      ).tupled.use{ case (rdf,builder) => for {
      schema <- RDF2Shacl.getShacl(rdf)
      eitherresult <- Validator.validate(schema, rdf)
      result <- either2io(eitherresult)
      (typing,ok) = result
      report <- typing.toValidationReport.toRDF(builder)
      strReport <- report.serialize("TURTLE")
    } yield (ok, strReport)}
    cmp.attempt.unsafeRunSync match {
      case Left(e) => fail(s"Error validating $name: $e")
      case Right(pair) => {
        val (ok,strReport) = pair
        info(s"Validation report: ${strReport}")
        ok should be(true)
      }
    }
  }

}
