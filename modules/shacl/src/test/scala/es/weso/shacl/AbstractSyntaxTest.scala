package es.weso.shacl

import munit._
import es.weso.rdf.nodes._

class AbstractSyntaxTest extends FunSuite {

  test("should be able to create a shape") {
    val x  = BNode("x")
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

    assertEquals(shape.id, id)

  }

}
