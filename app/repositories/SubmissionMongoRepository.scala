/*
 * Copyright 2025 HM Revenue & Customs
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

import com.google.inject.Singleton
import com.mongodb.client.model.IndexModel
import config.MicroserviceAppConfig
import org.bson.types.ObjectId
import model.{FlatSubmission, Submission}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.result.InsertOneResult

@Singleton
class SubmissionMongoRepository @Inject()(appConfig: MicroserviceAppConfig, mc: MongoComponent)
                                         (implicit ec: ExecutionContext)
  extends PlayMongoRepository[FlatSubmission](
    mongoComponent = mc,
    collectionName = appConfig.submissionCollectionName,
    domainFormat = FlatSubmission.formats,
    indexes = Seq.empty[IndexModel]
  ) {

  def storeSubmission(doc: Submission): Future[InsertOneResult] =
    collection
      .insertOne(FlatSubmission.fromSubmission(doc))
      .toFuture()

  def getOneSubmission(id: ObjectId): Future[Seq[FlatSubmission]] =
    collection
      .find(
        BsonDocument(
          "_id" -> id
        )
      )
      .toFuture()
}
