package peschke.happy

import cats.data.Chain
import org.http4s.Request

object FormatQueryParams {
  def forRequest[F[_]](req: Request[F]): Chain[String] = {
    if (req.uri.query.isEmpty) Chain.empty
    else
      Chain.fromSeq(req.uri.query.pairs)
        .map {
          case (key, None) => s"$key present"
          case (key, Some(value)) => s"$key=$value"
        }
  }
}
