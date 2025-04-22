package es.weso.shacl

import java.io.File
import com.typesafe.config.{Config, ConfigFactory}
import es.weso.rdf.jena.RDFAsJenaModel
import es.weso.shacl.converter.RDF2Shacl
import es.weso.shacl.validator.Validator
import es.weso.utils.FileUtils._

import scala.io.Source
import cats.effect._
import cats.implicits._
import munit.CatsEffectSuite

class ValidateFolderTest extends CatsEffectSuite {

  val conf: Config = ConfigFactory.load()
  val shaclFolder  = conf.getString("shaclTests")

  lazy val ignoreFiles: List[String] = List()

  def getTtlFiles(schemasDir: String): IO[List[File]] = {
    getFilesFromFolderWithExt(schemasDir, "ttl", ignoreFiles)
  }

  {
    val r = getTtlFiles(shaclFolder).map(files => files.map(name => validate(name))).void
    r.unsafeRunSync()
  }

  def validate(name: File): Unit = test(name.getAbsolutePath()) {
    val cmp = RDFAsJenaModel
      .fromFile(name, "TURTLE")
      .flatMap(
        _.use(rdf =>
          for {
            schema       <- RDF2Shacl.getShacl(rdf)
            eitherResult <- Validator.validate(schema, rdf)
            b <- eitherResult.fold(
              err => IO.raiseError(new RuntimeException(s"Error: ${err}")),
              result => {
                val (typing, ok) = result
                if (!ok) {
                  fail(s"Failed nodes: ${typing.t.getFailed}")
                } else
                  typing.t.allOk.pure[IO]
              }
            )
          } yield b
        )
      )

    assertIO(cmp, true)
  }

}
