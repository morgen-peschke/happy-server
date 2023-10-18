package peschke.happy

import cats.effect.kernel.{Async, Resource}
import cats.~>
import fs2.io.net.Network
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.http4s.server.middleware.Logger.httpApp
import org.typelevel.log4cats.LoggerFactory
import peschke.happy.TLSHelper.syntax._

object SetupServer {
  def run[F[_]: Async: Network: LoggerFactory](config: Config): F[Nothing] =
    SetupServer.setup[F](config).useForever

  def setup[F[_]: Async: Network: LoggerFactory](config: Config): Resource[F, Server] = {
    implicit val fToNT: F ~> Resource[F, *] = new (F ~> Resource[F, *]) {
      override def apply[A](fa: F[A]): Resource[F, A] = Resource.eval(fa)
    }
    val tlsHelper = config.tlsConfig.fold(TLSHelper.noOp[F, F])(TLSHelper.default[F](_)).mapK[Resource[F, *]]
    for {
      serverLogger <- Resource.eval(LoggerFactory[F].fromClass(classOf[Server]))
      server <- EmberServerBuilder
        .default[F]
        .withHost(config.host)
        .withPort(config.port)
        .withLogger(serverLogger)
        .withHttpApp(httpApp[F](logHeaders = false, logBody = false)(HappyApp.default[F]))
        .configureTLS(tlsHelper)
        .flatMap(_.build)
    } yield server
  }
}
