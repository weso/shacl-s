package es.weso.shacl

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should._
import es.weso.rdf.nodes._

class AbstractSyntaxTest extends AnyFunSpec with Matchers {

  describe("Abstract Syntax") {
    it("should be able to create a shape") {
      val x = BNode("x")
      val id = IRI("http://example.org/s")
      val shape = NodeShape(
        id = id,
        components = List(),
        targets = List(),
        propertyShapes = List(RefNode(x)),
        closed = false,
        List(),
        deactivated = false,
        MessageMap.empty,
        None,
        name = MessageMap.empty,
        description = MessageMap.empty,
        order = None,
        group = None,
        sourceIRI = None
      )

      shape.id should be(id)

    }

  }
}
