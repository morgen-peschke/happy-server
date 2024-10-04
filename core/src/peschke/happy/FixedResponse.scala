package peschke.happy

import org.http4s.{Header, Headers, Response, Status}
import org.typelevel.ci.CIString

final case class FixedResponse
  (
      status: Status,
      body: Option[String],
      headers: List[(CIString, String)]
  ) {
  def toHttp4sResponse[F[_]]: Response[F] = {
    val base = Response[F](status = status)
    val withBody = body.fold[Response[F]](base.withEmptyBody)(b => base.withBodyStream(fs2.Stream.emits(b.getBytes)))
    withBody.withHeaders(new Headers(headers.map { case n -> v =>
      Header.Raw(n, v)
    }))
  }
}
