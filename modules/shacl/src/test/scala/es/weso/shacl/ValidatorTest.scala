package es.weso.shacl

import es.weso.rdf.nodes._
import es.weso.rdf.jena.RDFAsJenaModel
import cats.effect._
import cats.implicits._
import util._
import es.weso.shacl.converter.RDF2Shacl
import es.weso.shacl.validator.Validator
import munit.CatsEffectSuite


class ValidatorTest extends CatsEffectSuite {

    test("should be able to obtain the target nodes to validate") {
      val ex = IRI("http://example.org/")
      val str = """|@prefix : <http://example.org/>
                 |@prefix sh: <http://www.w3.org/ns/shacl#>
                 |
                 |:S a sh:Shape; sh:targetNode :x, :y .
                 |:T a sh:Shape; sh:targetNode :z .
                 |""".stripMargin

      val S = ex + "S"
      val T = ex + "T"
      val x = ex + "x"
      val y = ex + "y"
      val z = ex + "z"
      val expected = List((y, S), (x, S),(z, T))
      val cmp = RDFAsJenaModel.fromString(str, "TURTLE").flatMap(_.use(rdf => for {
        schema <- RDF2Shacl.getShacl(rdf)
      } yield Validator(schema).targetNodes.map { case (node, shape) => (node, shape.id)}))
      assertIO(cmp, expected)
    }

    test("should be able to validate minCount") {
      val ex = IRI("http://example.org/")
      val str = """|@prefix : <http://example.org/>
                 |@prefix sh: <http://www.w3.org/ns/shacl#>
                 |
                 |:S a sh:NodeShape;
                 |   sh:targetNode :x ;
                 |   sh:property :PS .
                 |
                 |:PS sh:path :p ;
                 |    sh:minCount 1 .
                 |
                 |:x :p 1 .
                 |:good1 :p 1 .
                 |:good2 :p 1, 2 .
                 |:bad1 :q 1 .
                 |""".stripMargin
      val S = ex + "S"
      val PS = ex + "PS"
      val x = ex + "x"
     // val good1 = ex + "good1"
     // val good2 = ex + "good2"
     // val bad1 = ex + "bad1"
     // val ps = Shape.emptyPropertyShape(PS, PredicatePath(p)).copy(components = List(MinCount(1)))
      val psRefs = Seq(RefNode(PS))
      val s = Shape.empty(S).copy(
        targets = Seq(TargetNode(x)),
        propertyShapes = psRefs)
      val cmp = RDFAsJenaModel.fromString(str, "TURTLE").flatMap(_.use(rdf => for {
        schema <- RDF2Shacl.getShacl(rdf)
        validator = Validator(schema)
        checked <- validator.validateAll(rdf)
        _ <- assertEquals(validator.targetNodes, List((x,s))).pure[IO]
        _ <- assertEquals(checked.isOK,true).pure[IO]
      } yield (rdf, schema, validator,checked)))
  }
    

  test("minCount - validates minCount(1) when there is exactly 1") {
      val ex = IRI("http://example.org/")
      val str = s"""|@prefix : $ex
                |:x :p 1 .
                |""".stripMargin
      val cmp = RDFAsJenaModel.fromChars(str, "TURTLE").flatMap(_.use(rdf => {
        val validator = Validator(Schema.empty)
        for {
        checked <- validator.validateAll(rdf)
      } yield checked.isOK}))

      assertIO(cmp, true)
  }

    /*
 it("validates minCount(1) when there are 2") {
  val ex = IRI("http://example.org/")
  val str = s"""|@prefix : $ex
                |:x :p 1, 2 .
                |""".stripMargin
    val rdf = RDFAsJenaModel.fromChars(str,"TURTLE").get
    val x = ex + "x"
    val p = ex + "p"
    val validator = Validator(Schema.empty)
    validator.minCount(1).validateAll(x,(rdf,p)).isOK should be(true)
  }
 it("validates minCount(1) when there are 2 and other things...") {
  val ex = IRI("http://example.org/")
  val str = s"""|@prefix : $ex
                |:x :p 1, 2; :q 1 .
                |""".stripMargin
    val rdf = RDFAsJenaModel.fromChars(str,"TURTLE").get
    val x = ex + "x"
    val p = ex + "p"
    val validator = Validator(Schema.empty)
    validator.minCount(1).validateAll(x,(rdf,p)).isOK should be(true)
  }

  it("fails to validate minCount(2) when there are 1") {
  val ex = IRI("http://example.org/")
  val str = s"""|@prefix : $ex
                |:x :p 1; :q 1 .
                |""".stripMargin
    val rdf = RDFAsJenaModel.fromChars(str,"TURTLE").get
    val x = ex + "x"
    val p = ex + "p"
    val validator = Validator(Schema.empty)
    validator.minCount(2).validateAll(x,(rdf,p)).isOK should be(false)
  }

  it("fails to validate minCount(2) when there are no values") {
  val ex = IRI("http://example.org/")
  val str = s"""|@prefix : $ex
                |:x :q 1 .
                |""".stripMargin
    val rdf = RDFAsJenaModel.fromChars(str,"TURTLE").get
    val x = ex + "x"
    val p = ex + "p"
    val validator = Validator(Schema.empty)
    validator.minCount(2).validateAll(x,(rdf,p)).isOK should be(false)
  }

 it("fails to validate minCount(2) when there are no resource") {
  val ex = IRI("http://example.org/")
  val str = s"""|@prefix : $ex
                |:y :q 1 .
                |""".stripMargin
    val rdf = RDFAsJenaModel.fromChars(str,"TURTLE").get
    val x = ex + "x"
    val p = ex + "p"
    val validator = Validator(Schema.empty)
    validator.minCount(2).validateAll(x,(rdf,p)).isOK should be(false)
  }
*/
  /*
 describe("Property constraint"){
   it("validates minCount(1), maxCount(1) when there is exactly 1") {
     val ex = IRI("http://example.org/")
     val p = ex + "p"
     val x = ex + "x"
     val pc = PropertyConstraint(id = None, predicate = p, components =
       Seq(MinCount(1),MaxCount(1)))
     val validator = Validator.empty
     val str = s"""|@prefix : $ex
                   |:x :p "a" .
                   |""".stripMargin
     val rdf = RDFAsJenaModel.fromChars(str,"TURTLE").get
     val validated = validator.vPropertyConstraint(pc).validateAll(x,rdf)
     validated.isOK should be(true)
   }
 }

 describe("MinCount shape") {
   val strSchema = """|@prefix : <http://example.org/>
                 |@prefix sh: <http://www.w3.org/ns/shacl#>
                 |
                 |:S a sh:Constraint; sh:scopeNode :x;
                 |   sh:property [sh:predicate :p; sh:minCount 1] .
                 |""".stripMargin
   val attempt = for {
      rdf : RDFReader <- RDFAsJenaModel.fromChars(strSchema,"TURTLE")
      (schema,pm) <- RDF2Shacl.getShacl(rdf)
    } yield (rdf,schema)
   val (rdf,schema) = attempt.get
   val ex = IRI("http://example.org/")
   val s = ex + "S"

   it("validates a single shape") {
   val str = """|@prefix : <http://example.org/>
                |@prefix sh: <http://www.w3.org/ns/shacl#>
                |
                |:x :p "a" .
                |:S sh:scopeNode :x .
                |""".stripMargin
    val rdf = RDFAsJenaModel.fromChars(str,"TURTLE").get
    val validator = Validator(schema)
    val shape = schema.shape(s).value
    val result = validator.shapeConstraint.validateAll(shape,rdf)
    result.isOK should be(true)
   }

  it("fails to validate a single shape which doesn't satisfy minCount") {
   val str = """|@prefix : <http://example.org/>
                |@prefix sh: <http://www.w3.org/ns/shacl#>
                |
                |:y :q "a" .
                |:S sh:scopeNode :y .
                |""".stripMargin
    val rdf = RDFAsJenaModel.fromChars(str,"TURTLE").get
    val validator = Validator(schema)
    val shape = schema.shape(s).value
    val result = validator.shapeConstraint.validate(shape,rdf)
    result.isOK should be(false)
   }
 } */

}
