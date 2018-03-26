package config

import com.google.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.{AppName, BaseUrl}

@Singleton
class MicroserviceAppConfig @Inject()(override val configuration: Configuration) extends AppName with BaseUrl {

  private def loadConfig(key: String) = configuration.getString(key).getOrElse(throw new Exception(s"Missing configuration key: $key"))

  private def loadBoolean(key: String) = configuration.getBoolean(key).getOrElse(throw new Exception(s"Missing configuration key: $key"))

  private def loadMilliseconds(key : String, default : Long) = configuration.getMilliseconds(key).getOrElse(default)

  lazy val fileUploadUrl: String = baseUrl("file-upload")
  lazy val fileUploadFrontendUrl: String = baseUrl("file-upload-frontend")
  lazy val fileUploadCallbackUrl: String = loadConfig(s"microservice.services.file-upload.callbackUrl")

  lazy val pdfServiceUrl: String = baseUrl("pdf-generator-service")

  object Mongo {
    lazy val enabled: Boolean = loadBoolean(s"cache.isEnabled")
    lazy val encryptionEnabled: Boolean = loadBoolean(s"mongo.encryption.enabled")
    lazy val longLiveCacheExpiry: Long = loadMilliseconds("longLiveCache.expiryInMinutes", 1440L)
  }

  object SCC1 {

    lazy val businessArea : String = loadConfig(s"pdf.scc1.metadata.businessArea")
    lazy val queue : String = loadConfig(s"pdf.scc1.metadata.queue")
    lazy val formId : String = loadConfig(s"pdf.scc1.metadata.formId")
    lazy val source : String = loadConfig(s"pdf.scc1.metadata.source")
    lazy val target : String = loadConfig(s"pdf.scc1.metadata.target")
    lazy val save : Boolean = loadBoolean(s"pdf.scc1.save")

  }

}
