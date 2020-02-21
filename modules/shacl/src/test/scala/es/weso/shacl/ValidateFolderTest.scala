package es.weso.shacl

import java.io.File
import com.typesafe.config.{Config, ConfigFactory}
import es.weso.rdf.jena.RDFAsJenaModel
import es.weso.shacl.converter.RDF2Shacl
import es.weso.shacl.validator.Validator
import es.weso.utils.FileUtils._
import org.scalatest._
import scala.io.Source
import cats.data.EitherT
import cats.effect._
import cats.implicits._
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ValidateFolderTest
  extends AnyFunSpec with Matchers with TryValues with OptionValues
  with SchemaMatchers {

  val conf: Config = ConfigFactory.load()
  val shaclFolder = conf.getString("shaclTests")

  lazy val ignoreFiles: List[String] = List()

  def getTtlFiles(schemasDir: String): IO[List[File]] = {
    getFilesFromFolderWithExt(schemasDir, "ttl", ignoreFiles)
  }

  describe("Validate folder") {
    val files = getTtlFiles(shaclFolder)
    info(s"Validating files from folder $shaclFolder: $files")
    for (file <- getTtlFiles(shaclFolder).unsafeRunSync) {
      val name = file.getName
      it(s"Should validate file $name") {
        val str = Source.fromFile(file)("UTF-8").mkString
        validate(name, str)
      }
    }
  }

  def validate(name: String, str: String): Unit = {
    val attempt = for {
      rdf <- RDFAsJenaModel.fromStringIO(str, "TURTLE")
      schema <- RDF2Shacl.getShacl(rdf)
      result <- EitherT.fromEither[IO](Validator.validate(schema, rdf).leftMap(_.toString))
    } yield result
    attempt.fold(e => fail(s"Error validating $name: $e"),
      result => {
        val (typing,ok) = result
        if (!ok) {
          info(s"Failed nodes: ${typing.t.getFailed}")
        }
        typing.t.allOk should be(true)
      }
    )
  }

}
