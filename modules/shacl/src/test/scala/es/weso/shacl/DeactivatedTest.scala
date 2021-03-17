package es.weso.shacl

import es.weso.rdf.jena.RDFAsJenaModel
import es.weso.shacl.converter.RDF2Shacl
import es.weso.shacl.validator.Validator
import munit._

class DeactivatedTest extends CatsEffectSuite with SchemaMatchers {

  test("checks a deactivated shape") {
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

      val r = RDFAsJenaModel.fromString(str, "TURTLE", None).flatMap(_.use(rdf => for {
        schema <- RDF2Shacl.getShacl(rdf)
        result <- Validator.validate(schema, rdf)
      } yield result))

      r.attempt.map(v => assertEquals(v.isRight,true))

 } 
}
