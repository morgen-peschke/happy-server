package peschke.happy

import cats.effect.kernel.{Async, Resource}
import fs2.io.net.Network
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.http4s.server.middleware.Logger
import org.typelevel.log4cats.LoggerFactory

object SetupServer {
  def run[F[_]: Async: Network: LoggerFactory](config: Config): F[Nothing] =
    SetupServer.setup[F](config).useForever

  def setup[F[_]: Async: Network: LoggerFactory](config: Config): Resource[F, Server] = {
    val app = Logger.httpApp[F](logHeaders = true, logBody = false)(HappyApp.default[F])

    Resource.eval(LoggerFactory[F].fromClass(classOf[Server])).flatMap { serverLogger =>
      EmberServerBuilder
        .default[F]
        .withHost(config.host)
        .withPort(config.port)
        .withLogger(serverLogger)
        .withHttpApp(app)
        .build
    }
  }
}
