package es.weso.shacl

import es.weso.rdf.jena.RDFAsJenaModel
import es.weso.shacl.converter.RDF2Shacl
import es.weso.shacl.validator.Validator
import org.scalatest._
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should._

import cats.data.EitherT
import cats.effect._
import cats.implicits._
import es.weso.utils.IOUtils._

class DeactivatedTest extends AnyFunSpec with Matchers with TryValues with OptionValues
  with SchemaMatchers {

  describe("deactivated") {
    it("checks a deactivated shape") {
      val str =
        s"""|prefix : <http://e/>
            |prefix sh:     <http://www.w3.org/ns/shacl#>
            |prefix xsd:    <http://www.w3.org/2001/XMLSchema#>
            |prefix rdfs:   <http://www.w3.org/2000/01/rdf-schema#>
            |:PersonShape a sh:NodeShape ;
            |	 sh:targetNode :alice;
            |  sh:targetNode :bob ;
            |	 sh:property :HasName ;
            |  sh:property :HasAge .
            |:NotPerson a sh:NodeShape ;
            |  sh:not :PersonShape .
            |:HasName a sh:PropertyShape ; sh:path :name ;  sh:minCount 1 ; sh:deactivated true .
            |:HasAge a sh:PropertyShape ; sh:path :age ;  sh:minCount 1 .
            |
            |:alice a :Person; :age 35 .
            |:bob a :Person ; :age 23; :name "Robert" .
            |:carol a :Person .
            |:NotPerson sh:targetNode :carol .
            |  """.stripMargin

      val r = for {
        rdf    <- RDFAsJenaModel.fromString(str, "TURTLE", None)
        eitherSchema <- RDF2Shacl.getShacl(rdf).value
        schema <- eitherSchema match {
          case Left(s) => IO.raiseError(new RuntimeException(s"Error: $s"))
          case Right(s) => IO.pure(s)
        }
        result <- Validator.validate(schema, rdf)
      } yield result

      r.attempt.unsafeRunSync.fold(
        e => fail(s"Error reading: $e"),
        eitherResult => eitherResult match {
          case Left(ar) => fail(s"Error: $ar")
          case Right(pair) => info(s"Valid: ${pair}")
      })
    }
  }
}
