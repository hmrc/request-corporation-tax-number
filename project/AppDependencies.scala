import play.core.PlayVersion
import play.sbt.PlayImport.ws
import sbt.{ModuleID, _}

object AppDependencies {

  private val scope: String = "test,it"
  private val bootstrapPlayVersion: String = "7.14.0"
  private val scalaTestVersion: String = "3.2.9.0"

  private val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-backend-play-28" % bootstrapPlayVersion,
    "uk.gov.hmrc" %% "domain"                    % "8.3.0-play-28",
    "uk.gov.hmrc" %% "json-encryption"           % "5.1.0-play-28"
  )

  private val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-28" % bootstrapPlayVersion % scope,
    "com.typesafe.play"      %% "play-test"              % PlayVersion.current  % scope,
    "org.scalatestplus"      %% "mockito-3-4"            % scalaTestVersion     % scope,
    "org.scalatestplus"      %% "scalacheck-1-15"        % scalaTestVersion     % scope,
    "com.github.tomakehurst" % "wiremock-jre8"           % "2.35.0"             % scope,
    "org.jsoup"              % "jsoup"                   % "1.14.3"             % scope,
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.14.2"         % scope
  )

  def apply(): Seq[ModuleID] = compile ++ test

}
