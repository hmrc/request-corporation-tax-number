package connectors

import javax.inject.Singleton

import com.google.inject.Inject
import config.MicroserviceAppConfig
import play.Logger
import play.api.libs.json.{Json, Reads, Writes}
import play.modules.reactivemongo.MongoDbConnection
import uk.gov.hmrc.cache.TimeToLive
import uk.gov.hmrc.cache.model.Cache
import uk.gov.hmrc.cache.repository.CacheRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.{Duration, MINUTES}

class LongLiveCacheConnector @Inject()(
                                        val cacheRepository: EnrolmentCacheRepositoryHelper
                                      ) extends MongoDbConnection with TimeToLive {


  private val defaultKey = "SCC-iForm"

  def createOrUpdate[T](id: String, data: T, key: String = defaultKey)(implicit writes: Writes[T]): Future[T] = {
    cacheRepository.repo.createOrUpdate(id, key, Json.toJson(data)).map(_ => data)
  }

  def find[T](id: String, key: String = defaultKey)(implicit reads: Reads[T]): Future[Option[T]] = {
    cacheRepository.repo.findById(id) map {
      case Some(cache) => cache.data flatMap {
        json =>
          if ((json \ key).validate[T].isSuccess) {
            Some((json \ key).as[T])
          } else {
            None
          }
      }
      case None => None
    }
  }

  def removeById(id: String): Future[Boolean] = {
    for {
      writeResult <- cacheRepository.repo.removeById(id)
    } yield {
      if (writeResult.hasErrors) {
        writeResult.errmsg.foreach(Logger.error)
        throw new RuntimeException(writeResult.errmsg.getOrElse("Error while removing the session data"))
      } else {
        writeResult.ok
      }
    }
  }
}

@Singleton
class EnrolmentCacheRepositoryHelper @Inject()(appConfig: MicroserviceAppConfig) extends MongoDbConnection with TimeToLive {
  def expireAfter: Long = Duration(appConfig.Mongo.longLiveCacheExpiry, MINUTES).toSeconds

  val repo : CacheRepository = CacheRepository("SCC", expireAfter, Cache.mongoFormats)
}