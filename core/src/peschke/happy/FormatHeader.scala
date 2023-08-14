package peschke.happy

import cats.data.Chain
import org.http4s.Request

object FormatHeader {
  def forRequest[F[_]](req: Request[F]): Chain[String] =
    requestLine(req) +: headerLines(req)

  private def requestLine[F[_]](req: Request[F]): String =
    s"${req.method} ${req.uri} ${req.httpVersion}"

  private def headerLines[F[_]](req: Request[F]): Chain[String] =
    Chain.fromSeq(req.headers.headers).map { h =>
      s"${h.name}: ${h.value}"
    }
}
