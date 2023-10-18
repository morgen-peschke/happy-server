import mill._, scalalib._, scalafmt._
import mill.scalalib.publish._

trait CommonModule extends ScalaModule {
  override def scalaVersion: T[String] = "2.13.12"

  override def scalacOptions =
    super.scalacOptions() ++ Seq(
      "-encoding",
      "UTF-8",
      "-deprecation",
      "-unchecked",
      "-feature",
      "-Ywarn-unused",
      "-Ywarn-dead-code",
      "-Ywarn-value-discard",
      "-Xfatal-warnings",
      "-language:higherKinds"
    )

  override def scalaDocOptions = super.scalaDocOptions() ++ Seq("-no-link-warnings")

  override def scalacPluginIvyDeps = super.scalacPluginIvyDeps() ++ Agg(
    ivy"com.olegpy::better-monadic-for:0.3.1",
    ivy"org.typelevel:::kind-projector:0.13.2"
  )

}

object core extends ScalaModule with CommonModule with ScalafmtModule with PublishModule {

  override def ivyDeps =
    Agg(
      ivy"com.monovore::decline:2.3.0",
      ivy"is.cir::ciris:3.1.0",
      ivy"org.http4s::http4s-dsl:1.0.0-M40",
      ivy"org.http4s::http4s-ember-server:1.0.0-M40",
      ivy"org.typelevel::log4cats-slf4j:2.5.0"
    )

  override def runIvyDeps = Agg(ivy"ch.qos.logback:logback-classic:1.2.10")

  def publishVersion: T[String] = "0.1.0"

  override def pomSettings: T[PomSettings] = PomSettings(
    description = "Mock4s - mocked API server built on Http4s",
    organization = "com.github.morgen-peschke",
    url = "https://github.com/morgen-peschke/mock4s",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("morgen-peschke", "mock4s"),
    developers = Seq(
      Developer(
        "morgen-peschke",
        "Morgen Peschke",
        "https://github.com/morgen-peschke"
      )
    )
  )

  object test extends ScalaTests with CommonModule with TestModule.Munit {
    override def ivyDeps: T[Agg[Dep]] = super.ivyDeps() ++ Agg(
      ivy"org.scalacheck::scalacheck:1.17.0",
      ivy"org.scalameta::munit-scalacheck:0.7.29",
      ivy"org.scalameta::munit:0.7.29"
    )
  }
}
