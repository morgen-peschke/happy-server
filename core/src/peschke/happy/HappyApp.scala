package peschke.happy

import cats.data.Chain
import cats.effect.kernel.Concurrent
import cats.syntax.all._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.`Content-Type`
import org.http4s.multipart.Multipart
import org.typelevel.log4cats.LoggerFactory

object HappyApp {
  def default[F[_]: Concurrent: LoggerFactory]: HttpApp[F] = {
    val logger = LoggerFactory.getLogger[F]
    val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpApp[F] { request =>
      val requestCurl =
        s"""=========== Curl ===========
           |${request.asCurl(_ => false)}
           |""".stripMargin

      val queryInfo =
        if (request.uri.query.isEmpty) "=========== No Query String ==========="
        else
          Chain.fromSeq(request.uri.query.pairs)
            .map {
              case (key, None) => s"$key present"
              case (key, Some(value)) => s"$key=$value"
            }
            .mkString_("=========== Query Parameters ===========\n", "\n", "\n")

      val logHeader = Chain("Request Info:", requestCurl, queryInfo)

      request match {
        case (GET | DELETE) -> _ => logger.info(logHeader.mkString_("\n")) >> Ok()
        case _ =>
          request.headers.get[`Content-Type`].fold(MediaType.application.`octet-stream`)(_.mediaType) match {
            case MediaType.application.`x-www-form-urlencoded` =>
              request.decode[F, UrlForm] { form =>
                logger.info {
                  logHeader
                    .append("=========== Form Body ===========")
                    .concat(
                      Chain.fromSeq(form.values.toList).flatMap {
                        case (name, values) =>
                          Chain("---------- Part Name ----------", name)
                            .concat(values.flatMap(value => Chain("---------- Part Value ----------", value)))
                            .append("")
                      }
                    )
                    .mkString_("\n")
                } >> Ok()
              }

            case MediaType.multipart.`form-data` =>
              request.decode[F, Multipart[F]] { multipart =>
                Chain.fromSeq(multipart.parts)
                  .flatTraverse { part =>
                    part.bodyText.compile.string.map { value =>
                      part.name
                        .fold(Chain.one("---------- Unnamed Part ----------"))(Chain("---------- Part Name ----------", _))
                        .concat {
                          if (part.headers.isEmpty) Chain.empty
                          else
                            Chain.one("---------- Part Headers ----------")
                              .concat(Chain.fromSeq(part.headers.headers).map { header =>
                                s"${header.name}: ${header.value}"
                              })
                        }
                        .concat {
                          Chain.fromOption(part.filename).flatMap { filename =>
                            Chain("---------- Part Filename ----------", filename)
                          }
                        }
                        .concat(Chain("---------- Part Value ----------", value))
                    }
                  }
                  .flatMap { formInfo =>
                    logger.info {
                      logHeader
                        .append("=========== Multipart Body ===========")
                        .concat(formInfo)
                        .mkString_("\n")
                    } >> Ok()
                  }
              }

            case _ =>
              request.bodyText.compile.string.flatMap { body =>
                logger.info {
                  logHeader
                    .concat(Chain("=========== Body ===========", body))
                    .mkString_("\n")
                } >> Ok()
              }
          }
      }
    }
  }
}
