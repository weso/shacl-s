package es.weso.shacl

import java.io.File

import com.typesafe.config.{Config, ConfigFactory}
import es.weso.rdf.nodes.IRI
import es.weso.rdf.rdf4j.RDFAsRDF4jModel
import es.weso.shacl.converter.RDF2Shacl
import es.weso.shacl.validator.Validator
import es.weso.utils.FileUtils._
import org.scalatest._
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should._

import scala.io.Source
import cats.data.EitherT
import cats.effect._

import scala.util._
import cats.implicits._
import es.weso.utils.IOUtils2.either2io

class ValidateFolder_RDF4jTest extends AnyFunSpec with Matchers with TryValues with OptionValues
  with SchemaMatchers {

  val conf: Config = ConfigFactory.load()
  val shaclFolder = conf.getString("shaclTests")

  lazy val ignoreFiles: List[String] = List()

  def getTtlFiles(schemasDir: String): IO[List[File]] = {
    getFilesFromFolderWithExt(schemasDir, "ttl", ignoreFiles)
  }

  describe("Validate folder") {
    val files = getTtlFiles(shaclFolder).unsafeRunSync
    info(s"Validating files from folder $shaclFolder: $files")
    for (file <- files) {
      val name = file.getName
      it(s"Should validate file $name") {
        val str = Source.fromFile(file)("UTF-8").mkString
        validate(name, str)
      }
    }
  }

  def validate(name: String, str: String): Unit = {
    val cmp = for {
      rdf <- RDFAsRDF4jModel.fromChars(str, "TURTLE", Some(IRI("http://example.org/")))
      schema <- RDF2Shacl.getShacl(rdf)
      eitherResult <- Validator.validate(schema, rdf)
      result <- either2io(eitherResult)
    } yield result
    cmp.attempt.unsafeRunSync match {
      case Left(e) => fail(s"Error validating $name: $e")
      case Right(typing) => info(s"Validated $name")
    }
  }

}
