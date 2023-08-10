import play.sbt.PlayImport.ws
import sbt._

object AppDependencies {

  private val bootstrapPlayVersion: String = "7.21.0"

  private val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-backend-play-28" % bootstrapPlayVersion,
    "uk.gov.hmrc" %% "domain"                    % "8.3.0-play-28",
    "uk.gov.hmrc" %% "json-encryption"           % "5.1.0-play-28"
  )

  private val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-28"      % bootstrapPlayVersion,
    "org.scalatest"          %% "scalatest"                   % "3.2.16",
    "org.scalatestplus"      %% "scalacheck-1-17"             % "3.2.16.0",
    "org.scalatestplus"      %% "mockito-4-11"                % "3.2.16.0",
    "com.github.tomakehurst" %  "wiremock-standalone"         % "2.27.2",
    "org.jsoup"              %  "jsoup"                       % "1.16.1",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"  % "2.15.2",
    "com.vladsch.flexmark"   % "flexmark-all"                 % "0.64.8"
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test

}
