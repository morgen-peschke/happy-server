package peschke.happy

import cats.MonadThrow
import cats.data.{NonEmptyList, Validated}
import cats.effect.kernel.Async
import cats.effect.std.Console
import cats.syntax.all._
import ciris.ConfigDecoder
import com.comcast.ip4s
import com.comcast.ip4s.{Host, Ipv6Address, Port}
import com.monovore.decline.{Argument, Command, Opts}
import fs2.io.file.Path
import peschke.happy.Config.TLSConfig

final case class Config(host: Host, port: Port, tlsConfig: Option[TLSConfig])
object Config {
  final case class TLSConfig(keyStorePath: Path,
                             storePassword: String,
                             keyPassword: String)

  trait Loader[F[_]] {
    def load: F[Config]
  }

  private val DefaultHost: Ipv6Address = ip4s.Ipv6Address.fromBigInt(BigInt(1))

  def parse[F[_]: MonadThrow: Console](args: Seq[String]): Loader[F] = new Loader[F] {
    private implicit val hostArg: Argument[Host] = Argument.from("host") { raw =>
      Host.fromString(raw).toValidNel(s"Unable to parse host from $raw")
    }

    private implicit val portArg: Argument[Port] = Argument.from("port") { raw =>
      Port.fromString(raw).toValidNel(s"Unable to parse port from $raw")
    }

    private implicit val pathArg: Argument[Path] = Argument.from("path") { raw =>
      Validated.catchNonFatal(Path(raw)).leftMap(_.getMessage.pure[NonEmptyList])
    }

    private val hostOpt =
      Opts
        .option[Host](long = "host", help = "The host to bind (defaults to localhost)")
        .orElse(DefaultHost.pure[Opts])

    private val portOpt = Opts.option[Port](long = "port", help = "The port to listen on")

    private def die: F[Config] = new IllegalArgumentException().raiseError[F, Config]

    private val tlsStorePathOpt: Opts[Path] =
      Opts
        .option[Path](long = "tls-store", help = "The path to the keystore file, if running ssl")

    private val tlsStorePassOpt: Opts[String] =
      Opts.option[String](long = "tls-store-pass", help = "The store password")

    private val tlsKeyPassOpt: Opts[String] =
      Opts.option[String](long = "tls-key-pass", help = "The key password")

    private val tlsPassOpt: Opts[(String, String)] =
      Opts.option[String](long = "tls-pass", help = "The store & key password (if identical)").map(p => (p, p))

    private val tlsConfig: Opts[Option[TLSConfig]] =
      (
        tlsStorePathOpt,
        (tlsStorePassOpt, tlsKeyPassOpt).tupled.orElse(tlsPassOpt)
      ).mapN { case (path, (storePass, keyPass)) =>
        TLSConfig(path, storePass, keyPass)
      }.orNone

    override def load: F[Config] = {
      val command = Command(name = "happy-server", header = "Happy API Server")(
        (hostOpt, portOpt, tlsConfig).mapN(Config.apply)
      )
      command.parse(args, sys.env).fold(Console[F].errorln(_) >> die, _.pure[F])
    }
  }

  def default[F[_]: Async]: Loader[F] = new Loader[F] {
    private implicit val hostDecoder: ConfigDecoder[String, Host] =
      ConfigDecoder[String].mapOption("Host")(Host.fromString)

    private implicit val portDecoder: ConfigDecoder[String, Port] =
      ConfigDecoder[String].mapOption("Port")(Port.fromString)

    private implicit val pathDecoder: ConfigDecoder[String, Path] =
      ConfigDecoder[String].mapOption("Path")(str => Either.catchNonFatal(Path(str)).toOption)

    override def load: F[Config] = {
      val host = ciris.env("HAPPY_SERVER_HOST").as[Host].default(DefaultHost)
      val port = ciris.env("HAPPY_SERVER_PORT").as[Port]
      val tlsStorePath = ciris.env("HAPPY_SERVER_TLS_STORE_PATH").as[Path]
      val tlsStorePass = ciris.env("HAPPY_SERVER_TLS_STORE_PASS").as[String]
      val tlsKeyPass = ciris.env("HAPPY_SERVER_TLS_KEY_PASS").as[String]
      (
        host,
        port,
        (tlsStorePath, tlsStorePass, tlsKeyPass).mapN(TLSConfig.apply).option
      ).mapN(Config.apply).load[F]
    }
  }
}
