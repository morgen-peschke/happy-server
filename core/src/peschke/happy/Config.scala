package peschke.happy

import cats.MonadThrow
import cats.effect.kernel.Async
import cats.effect.std.Console
import cats.syntax.all._
import ciris.ConfigDecoder
import com.comcast.ip4s
import com.comcast.ip4s.{Host, Ipv6Address, Port}
import com.monovore.decline.{Argument, Command, Opts}

final case class Config(host: Host, port: Port)
object Config {
  trait Loader[F[_]] {
    def load: F[Config]
  }

  private val DefaultHost: Ipv6Address = ip4s.Ipv6Address.fromBigInt(BigInt(1))

  def parse[F[_]: MonadThrow: Console](args: Seq[String]): Loader[F] = new Loader[F] {
    implicit val hostArg: Argument[Host] = Argument.from("host") { raw =>
      Host.fromString(raw).toValidNel(s"Unable to parse host from $raw")
    }

    implicit val portArg: Argument[Port] = Argument.from("port") { raw =>
      Port.fromString(raw).toValidNel(s"Unable to parse port from $raw")
    }

    private val hostOpt =
      Opts
        .option[Host](long = "host", help = "The host to bind (defaults to localhost)")
        .orElse(DefaultHost.pure[Opts])

    private val portOpt = Opts.option[Port](long = "port", help = "The port to listen on")

    private def die: F[Config] = new IllegalArgumentException().raiseError[F, Config]

    override def load: F[Config] = {
      val command = Command(name = "happy-server", header = "Happy API Server")(
        (hostOpt, portOpt).mapN(Config.apply)
      )
      command.parse(args, sys.env).fold(Console[F].errorln(_) >> die, _.pure[F])
    }
  }

  def default[F[_]: Async]: Loader[F] = new Loader[F] {
    private implicit val hostDecoder: ConfigDecoder[String, Host] =
      ConfigDecoder[String].mapOption("Host")(Host.fromString)

    private implicit val portDecoder: ConfigDecoder[String, Port] =
      ConfigDecoder[String].mapOption("Port")(Port.fromString)

    override def load: F[Config] = {
      val host = ciris.env("HAPPY_SERVER_HOST").as[Host].default(DefaultHost)
      val port = ciris.env("HAPPY_SERVER_PORT").as[Port]
      (host, port).mapN(Config.apply).load[F]
    }
  }
}
