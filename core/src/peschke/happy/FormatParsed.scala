package peschke.happy

import cats.data.Chain
import cats.effect.kernel.Concurrent
import cats.syntax.all._
import org.http4s._
import org.http4s.headers.`Content-Type`
import org.http4s.multipart.Multipart

object FormatParsed {
  def request[F[_] : Concurrent](req: Request[F]): F[String] =
    req.entity match {
      case Entity.Empty => "<Empty Body>".pure[F]
      case _ =>
        val mediaType = req.headers.get[`Content-Type`].fold(MediaType.application.`octet-stream`)(_.mediaType)

        if (mediaType === MediaType.application.`x-www-form-urlencoded`)
          EntityDecoder[F, UrlForm].decode(req, strict = false).value.flatMap(_.liftTo[F]).map { form =>
            ("========== Form Body ==========" +: Chain.fromSeq(form.values.toList).flatMap {
              case (name, values) =>
                values.map(value => s"$name=$value")
            }).mkString_("\n")
          }

        else if (mediaType.isMultipart)
          EntityDecoder[F, Multipart[F]].decode(req, strict = false).value.flatMap(_.liftTo[F]).flatMap { multipart =>
            Chain.fromSeq(multipart.parts)
              .flatTraverse { part =>
                (part.entity match {
                  case Entity.Empty => Chain.one("<Empty Body>").pure[F]
                  case _ => part.bodyText.compile.string.map(Chain("", _))
                }).map { bodyInfo =>
                  part.name
                    .fold(Chain.one("---------- Unnamed Part ----------"))(n => Chain(s"---------- $n ----------"))
                    .concat {
                      if (part.headers.isEmpty) Chain.empty
                      else
                        Chain.fromSeq(part.headers.headers).map { header =>
                          s"${header.name}: ${header.value}"
                        }
                    }
                    .concat {
                      Chain.fromOption(part.filename).flatMap { filename =>
                        Chain("File Name:", filename)
                      }
                    }
                    .concat(bodyInfo)
                }
              }
              .map { formInfo =>
                  ("========== Multipart Body ==========" +: formInfo).mkString_("\n")
              }
          }
        else
          req.bodyText.compile.string.map { body =>
            s"========== Body ==========\n$body"
          }
    }
}
