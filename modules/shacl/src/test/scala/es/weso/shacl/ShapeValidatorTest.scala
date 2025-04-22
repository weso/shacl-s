package es.weso.shacl

// import es.weso.rdf.nodes._
import es.weso.rdf.jena.RDFAsJenaModel
import munit._

import util._
import es.weso.shacl.converter.RDF2Shacl
import es.weso.shacl.validator.Validator
// import cats.data.EitherT
import cats.effect._

// import cats.implicits._
// import es.weso.utils.IOUtils

class ShapeValidatorTest extends CatsEffectSuite {

  test("Should validate single shape") {
    /*val str = """|@prefix : <http://example.org/>
                 |@prefix sh: <http://www.w3.org/ns/shacl#>
                 |@prefix xsd: <http://www.w3.org/2001/XMLSchema#>
                 |
                 |:S a sh:Shape;
                 |   sh:targetNode :x;
                 |   sh:property [sh:path :p; sh:datatype xsd:string; sh:minCount 1] ;
                 |   sh:property [sh:path :q; sh:datatype xsd:integer; sh:minCount 1] .
                 |:x :p "23"; :q 33 .
                 |""".stripMargin */
    val str = """|prefix :       <http://example.org/> 
                 |prefix sh:     <http://www.w3.org/ns/shacl#> 
                 |prefix xsd:    <http://www.w3.org/2001/XMLSchema#>
                 |
                 |# Separation test reach1
                 |:R a sh:NodeShape ;
                 |   sh:targetNode :a ;
                 |   sh:property 
                 |     [ sh:path :p ;
                 |       sh:qualifiedValueShape :R ;
                 |       sh:qualifiedMinCount 1
                 |     ] .
                 |
                 |:a :p :a .
                 |""".stripMargin
    val cmp = RDFAsJenaModel
      .fromString(str, "TURTLE")
      .flatMap(
        _.use(rdf =>
          for {
            schema       <- RDF2Shacl.getShacl(rdf)
            eitherResult <- Validator.validate(schema, rdf)
            _            <- IO.println(s"Either result: $eitherResult")
            result <- eitherResult
              .fold(err => IO.raiseError(new RuntimeException(s"Error validating: ${err.toString}")), IO.pure(_))
          } yield (result._2)
        )
      )
    cmp.map(result => assertEquals(result, true))
  }
}
