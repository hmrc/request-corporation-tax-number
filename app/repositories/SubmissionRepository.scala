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

package repositories

import javax.inject.Inject

import com.google.inject.Singleton
import connectors.LongLiveCacheConnector
import model.{SubmissionDetails}
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

/**
  * Find, update and remove a form in mongo
  */

@Singleton
class SubmissionRepository @Inject()(
                                   val longLiveCacheConnector: LongLiveCacheConnector
                                   ) {

  private val defaultKey = "CTUTR"

  def submissionDetails(id: String)(implicit hc: HeaderCarrier): Future[Option[SubmissionDetails]] = {
    Logger.info(s"[SubmissionRepository][submissionDetails][searching for submission in mongo $id")
    longLiveCacheConnector.find[SubmissionDetails](id, defaultKey)
  }

  def updateSubmissionDetails(id: String, details: SubmissionDetails)(implicit hc: HeaderCarrier): Future[SubmissionDetails] = {
    Logger.info(s"[SubmissionRepository][updateSubmissionDetails][updating submission in mongo $id")
    longLiveCacheConnector.createOrUpdate[SubmissionDetails](id, details, defaultKey)
  }

  def removeSubmissionDetails(id: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    Logger.info(s"[SubmissionRepository][removeSubmissionDetails][removing submission from mongo $id")
    longLiveCacheConnector.removeById(id)
  }

}