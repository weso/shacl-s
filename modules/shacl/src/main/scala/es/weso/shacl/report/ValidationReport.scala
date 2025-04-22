package es.weso.shacl.report

import es.weso.rdf.RDFBuilder
import es.weso.rdf.saver.RDFSaver
import cats.effect.IO

case class ValidationReport(conforms: Boolean, results: Seq[AbstractResult], shapesGraphWellFormed: Boolean)
    extends RDFSaver {

  def toRDF(builder: RDFBuilder): IO[RDFBuilder] = {
    ValidationReport2RDF.run(this, builder)
  }

}

object ValidationReport {
  def fromError(e: AbstractResult): ValidationReport = {
    ValidationReport(conforms = false, results = Seq(e), true)
  }
}
