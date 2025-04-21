package es.weso.shacls

import cats.effect.IOApp
import com.typesafe.scalalogging.LazyLogging
import cats.effect.IO
import cats.effect.ExitCode

object Main extends IOApp with LazyLogging {
  override def run(args: List[String]): IO[ExitCode] = {
    logger.info("Starting application...")
    // Your application logic here
    IO(ExitCode.Success)
  }
}
