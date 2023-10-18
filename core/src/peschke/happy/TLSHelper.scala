package peschke.happy

import cats.{Applicative, Functor, ~>}
import cats.syntax.all._
import fs2.io.net.Network
import org.http4s.ember.server.EmberServerBuilder
import peschke.happy.Config.TLSConfig

trait TLSHelper[F[_], C[_]] {
  def configure(builder: EmberServerBuilder[C]): F[EmberServerBuilder[C]]

  def mapK[F1[_]](implicit nt: F ~> F1): TLSHelper[F1, C] = {
    val f = this
    builder => nt(f.configure(builder))
  }
}
object TLSHelper {
  def noOp[F[_] : Applicative, C[_]]: TLSHelper[F, C] = _.pure[F]

  def default[F[_] : Network : Functor](tlsConfig: TLSConfig): TLSHelper[F, F] =
    builder =>
      Network[F].tlsContext.fromKeyStoreFile(
          tlsConfig.keyStorePath.toNioPath,
          tlsConfig.storePassword.toCharArray,
          tlsConfig.keyPassword.toCharArray
        )
        .map(tlsContext => builder.withTLS(tlsContext))

  object syntax {
    implicit final class TLSHelperOps[C[_]](private val builder: EmberServerBuilder[C]) extends AnyVal {
      def configureTLS[F[_]](tlsHelper: TLSHelper[F, C]): F[EmberServerBuilder[C]] =
        tlsHelper.configure(builder)
    }
  }
}
