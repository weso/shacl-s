package es.weso.shacl

import es.weso.rdf.jena.RDFAsJenaModel
import es.weso.rdf.nodes._
import es.weso.shacl.converter.RDF2Shacl
import org.scalatest._
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should._


class SiblingsTest extends AnyFunSpec
  with Matchers with EitherValues with OptionValues {

  val ex = "http://example.org/"
  val str: String =
    s"""|prefix :       <$ex>
        |prefix sh:     <http://www.w3.org/ns/shacl#>
        |prefix xsd:    <http://www.w3.org/2001/XMLSchema#>
        |prefix schema: <http://schema.org/>
        |prefix foaf:   <http://xmlns.com/foaf/0.1/>
        |prefix rdfs:   <http://www.w3.org/2000/01/rdf-schema#>
        |
         |:Marriage a sh:NodeShape ;
        |    sh:property :PsMale ;
        |    sh:property :PsFemale .
        |
         |:PsMale a sh:PropertyShape ;
        |      sh:path :member ;
        |      sh:qualifiedMinCount 1 ;
        |      sh:qualifiedMaxCount 1 ;
        |      sh:qualifiedValueShape :MaleShape ;
        |      sh:qualifiedValueShapesDisjoint true .
        |
         |:PsFemale a sh:PropertyShape ;
        |      sh:path :member ;
        |      sh:qualifiedMinCount 1 ;
        |      sh:qualifiedMaxCount 1 ;
        |      sh:qualifiedValueShape :FemaleShape ;
        |      sh:qualifiedValueShapesDisjoint true .
        |
         |:MaleShape a sh:NodeShape ;
        |  sh:property [
        |   sh:path :gender ;
        |   sh:hasValue :male
        |  ] .
        |
         |:FemaleShape a sh:NodeShape ;
        | sh:property [
        |  sh:path :gender ;
        |  sh:hasValue :female
        |] .
        |""".stripMargin
  val psFemale: IRI = IRI(ex + "PsFemale")
  val psMale: IRI = IRI(ex + "PsMale")
  val maleShape: IRI = IRI(ex + "MaleShape")
  val femaleShape: IRI = IRI(ex + "FemaleShape")
  val marriage: IRI = IRI(ex + "Marriage")

  describe("Parent") {

    it("should be able to find parent of a shape") {
      val eitherParents = RDFAsJenaModel.fromString(str, "TURTLE").use(rdf => for {
        schema <- RDF2Shacl.getShacl(rdf)
      } yield schema.parents(RefNode(psFemale)))

      eitherParents.attempt.unsafeRunSync match {
        case Right(ps) =>
          info(s"Parents found: $ps")
          ps should contain only RefNode(marriage)
        case Left(e) => fail(e)
      }
    }

    describe("SiblingQualifiedValueShapes") {
      it("should be able to find siblings of a shape") {

        val eitherShapes = RDFAsJenaModel.fromString(str, "TURTLE").use(rdf => for {
          schema <- RDF2Shacl.getShacl(rdf)
        } yield schema.siblingQualifiedShapes(RefNode(psFemale)))

        eitherShapes.attempt.unsafeRunSync match {
          case Right(ss) =>
            info(s"Siblings found: $ss")
            ss.map(_.id) should contain only maleShape
          case Left(msg) => fail(msg)
        }
      }
    }
  }

}
