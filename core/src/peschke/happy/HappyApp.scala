package peschke.happy

import cats.data.Chain
import cats.effect.kernel.Concurrent
import cats.syntax.all._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.typelevel.log4cats.LoggerFactory

object HappyApp {
  def default[F[_]: Concurrent: LoggerFactory]: HttpApp[F] = {
    val logger = LoggerFactory.getLogger[F]
    val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpApp[F] { request =>
      val header = "########## Head ##########" +: FormatHeader.forRequest(request)
      val queryParams = {
        val qp = FormatQueryParams.forRequest(request)
        if (qp.isEmpty) qp
        else "########## Query Params ##########" +: qp
      }
      for {
        case (raw, request) <- FormatRaw.request(request)
        parsed <- FormatParsed.request(request)
        _ <- logger.info({
          ("Request Info" +: header) ++
            queryParams ++
            Chain("########## Raw Body ##########", raw) ++
            Chain("########## Parsed Body ##########", parsed)
        }.mkString_("\n"))
        res <- Ok()
      } yield res
    }
  }
}
