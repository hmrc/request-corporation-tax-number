/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
                                        val cacheRepository: SubmissionCacheRepositoryHelper
                                      ) extends MongoDbConnection with TimeToLive {

  def createOrUpdate[T](id: String, data: T, key: String)(implicit writes: Writes[T]): Future[T] = {
    cacheRepository.repo.createOrUpdate(id, key, Json.toJson(data)).map(_ => data)
  }

  def find[T](id: String, key: String)(implicit reads: Reads[T]): Future[Option[T]] = {
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
class SubmissionCacheRepositoryHelper @Inject()(appConfig: MicroserviceAppConfig) extends MongoDbConnection with TimeToLive {
  def expireAfter: Long = Duration(appConfig.Mongo.longLiveCacheExpiry, MINUTES).toSeconds

  val repo : CacheRepository = CacheRepository("CTUTR", expireAfter, Cache.mongoFormats)
}