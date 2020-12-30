package es.weso.shacl

import java.nio.file.Paths
import com.typesafe.config.{Config, ConfigFactory}
import es.weso.rdf.jena.RDFAsJenaModel
import es.weso.rdf.nodes.IRI
import es.weso.shacl.converter.RDF2Shacl
import es.weso.shacl.validator.Validator
import org.scalatest._
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should._
import cats.effect._

class ImportTest extends AnyFunSpec with Matchers with TryValues with OptionValues
  with SchemaMatchers {

  val conf: Config = ConfigFactory.load()
  val shaclFolderStr = conf.getString("shaclTests")
  val shaclFolder = IRI(Paths.get(shaclFolderStr).normalize.toUri.toString)  + "imports/"

  describe("import") {
    it(s"Validates a shape that imports another one. ShaclFolder: ${shaclFolder}") {
      val r = 
        RDFAsJenaModel.fromIRI(iri = shaclFolder + "import.ttl", format = "TURTLE", base = Some(shaclFolder)).flatMap(_.use(
         rdf => for {
        //_ <- { println(s"RDF: ${rdf.serialize("TURTLE").getOrElse("<None>")}"); Right(()) } 
        // extendedRdf <- rdf.extendImports()
        // _ <- { println(s"Extended RDF: ${extendedRdf.serialize("TURTLE").getOrElse("<None>")}"); Right(()) } 
        schema <- RDF2Shacl.getShacl(rdf)
        //_ <- { println(s"----\nSchema: ${schema.serialize("TURTLE", None,RDFAsJenaModel.empty)}"); Right(()) } 
        eitherResult <- Validator.validate(schema, rdf)
        result <- eitherResult.fold(s => IO.raiseError(new RuntimeException(s"Error validating: $s")),IO.pure(_))
      } yield result))

      r.attempt.unsafeRunSync.fold(
        e => fail(s"Error reading: $e"),
        pair => {
        val (typing, ok) = pair
        val alice = IRI("http://example.org/alice")
        val bob = IRI("http://example.org/bob")
        val person = IRI("http://example.org/Person")
        val hasName = IRI("http://example.org/hasName")
        typing.getFailedValues(alice).map(_.id) should contain theSameElementsAs List()
        typing.getFailedValues(bob).map(_.id) should contain theSameElementsAs List(person,hasName)
      })
    }

/*    it(s"Validates a shape that imports another one with a loop") {
      val r = for {
        rdf    <- RDFAsJenaModel.fromIRI(shaclFolder + "imports/importWithLoop.ttl")
        schema <- RDF2Shacl.getShacl(rdf)
        result <- Validator.validate(schema, rdf).leftMap(ar => s"AbstractResult: $ar")
      } yield result

      r.fold(
        e => fail(s"Error reading: $e"),
        pair => {
          val (typing, ok) = pair
          val alice = IRI("http://example.org/alice")
          val bob = IRI("http://example.org/bob")
          val person = IRI("http://example.org/Person")
          val hasName = IRI("http://example.org/hasName")
          typing.getFailedValues(alice).map(_.id) should contain theSameElementsAs(List())
          typing.getFailedValues(bob).map(_.id) should contain theSameElementsAs(List(person,hasName))
        })
    } */
  }  
}
