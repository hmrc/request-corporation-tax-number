package config

import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}

trait SpecBase extends PlaySpec with OneAppPerSuite {

  def injector = app.injector

  def appConfig : MicroserviceAppConfig = injector.instanceOf[MicroserviceAppConfig]
}
