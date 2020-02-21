package es.weso.shacl

import es.weso.rdf.jena.RDFAsJenaModel
import es.weso.shacl.converter.RDF2Shacl
import es.weso.shacl.validator.Validator
import org.scalatest._
import cats.data.EitherT
import cats.effect._
import cats.implicits._
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

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
        rdf    <- RDFAsJenaModel.fromStringIO(str, "TURTLE", None)
        schema <- RDF2Shacl.getShacl(rdf)
        result <- EitherT.fromEither[IO](Validator.validate(schema, rdf).leftMap(_.toString))
      } yield result

      r.value.unsafeRunSync.fold(
        e => fail(s"Error reading: $e"),
        pair => {
        val (typing, ok) = pair
        if (ok) {
          info(s"Valid as expected")
        } else {
          fail(s"Not valid. Typing:\n$typing")
        }
      })
    }
  }
}
