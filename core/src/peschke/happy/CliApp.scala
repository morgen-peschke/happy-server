package peschke.happy

import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.syntax.all._
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

object CliApp extends IOApp {

  implicit val logging: LoggerFactory[IO] = Slf4jFactory.create[IO]

  override def run(args: List[String]): IO[ExitCode] = {
    val logger = LoggerFactory.getLogger[IO]

    Config
      .parse[IO](args).load.redeemWith(
        logger.error(_)("Unable to start up") >> ExitCode.Error.pure[IO],
        SetupServer.run[IO](_).as(ExitCode.Success)
      )
  }
}
