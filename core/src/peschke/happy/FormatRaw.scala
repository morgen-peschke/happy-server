package peschke.happy

import cats.syntax.all._
import cats.effect.kernel.Concurrent
import fs2.text
import org.http4s.{Charset, Request}

object FormatRaw {
  def request[F[_]: Concurrent](req: Request[F]): F[(String, Request[F])] = {
    for {
      bodyBytes  <- req.body.compile.toVector.map(fs2.Stream.emits(_))
      bodyString <- {
        val cs = req.charset.getOrElse(Charset.`UTF-8`).nioCharset
        bodyBytes.through(text.decodeWithCharset(cs)).compile.string
      }
    } yield (bodyString, req.withBodyStream(bodyBytes))
  }
}
