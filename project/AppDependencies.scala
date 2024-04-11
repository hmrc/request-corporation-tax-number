import play.sbt.PlayImport.ws
import sbt._

object AppDependencies {

  private val bootstrapPlayVersion: String = "8.5.0"

  private val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-backend-play-30" % bootstrapPlayVersion,
    "uk.gov.hmrc" %% "domain-play-30"            % "9.0.0",
    "uk.gov.hmrc" %% "json-encryption"   % "5.3.0-play-28"
  )

  private val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-30"      % bootstrapPlayVersion,
    "org.scalatest"          %% "scalatest"                   % "3.2.18",
    "org.scalatestplus"      %% "scalacheck-1-17"             % "3.2.18.0",
    "org.scalatestplus"      %% "mockito-5-10"                % "3.2.18.0",
    "org.wiremock"           %  "wiremock-standalone"         % "3.5.2",
    "org.jsoup"              %  "jsoup"                       % "1.17.2",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"  % "2.17.0",
    "com.vladsch.flexmark"   % "flexmark-all"                 % "0.64.8"
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test

}
