package es.weso.shacl
import cats._
import es.weso.rdf.nodes._
import es.weso.shacl.report.{AbstractResult, MsgError, ValidationResult}

object showShacl {

  implicit def showShape: Show[Shape] = new Show[Shape] {
    def show(shape: Shape): String = {
      shape.id.toString // .fold("_?")(iri => iri.str)
    }
  }

  implicit def showError: Show[AbstractResult] = new Show[AbstractResult] {
    def show(ve: AbstractResult): String =
      ve match {
        case vr: ValidationResult =>
          s"Violation Error(${vr.sourceConstraintComponent}). Node(${vr.focusNode}) ${vr.message.mkString(",")}"
        case m: MsgError => s"Error: ${m.msg}"
      }
  }

  implicit def showRDFNode: Show[RDFNode] = new Show[RDFNode] {
    def show(n: RDFNode): String = n.toString
  }

}
