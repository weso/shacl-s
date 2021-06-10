package es.weso.utils
import cats.effect.IO

object IOUtils2 {
  def either2io[E,A](e: Either[E,A]): IO[A] = {
    e.fold(err => IO.raiseError(new RuntimeException(err.toString)), IO.pure(_))
  }
}