import play.sbt.PlayImport.ws
import sbt.*

object AppDependencies {

  private val bootstrapPlayVersion: String = "9.16.0"
  private val mongoVersion: String = "2.9.0"

  private val compile = Seq(
    ws,
    "uk.gov.hmrc"            %% "bootstrap-backend-play-30" % bootstrapPlayVersion,
    "uk.gov.hmrc"            %% "domain-play-30"            % "11.0.0",
    "org.apache.xmlgraphics" % "fop"                        % "2.11",
    "net.sf.saxon"           % "Saxon-HE"                   % "12.7",
    "commons-io"             % "commons-io"                 % "2.20.0",
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-play-30"       % mongoVersion
  )

  private val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                  %% "bootstrap-test-play-30"      % bootstrapPlayVersion,
    "org.scalatestplus"            %% "scalacheck-1-18"             % "3.2.19.0",
    "org.apache.pdfbox"            %  "pdfbox"                      % "3.0.5",
    "uk.gov.hmrc.mongo"            %% "hmrc-mongo-test-play-30"     % mongoVersion
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test

}
