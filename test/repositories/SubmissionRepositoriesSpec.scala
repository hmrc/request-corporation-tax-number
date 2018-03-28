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

import connectors.LongLiveCacheConnector
import model.SubmissionDetails
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.SessionId

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class SubmissionRepositoriesSpec extends PlaySpec with MockitoSugar {

  private implicit val hc = HeaderCarrier(sessionId = Some(SessionId("TESTING")))

  "iFormDetails" should {
    "return submissionDetails" when {
      "cache returns data" in {
        val sut = createSUT
        val submissionDetails = SubmissionDetails(pdfUploaded = false, metadataUploaded = false)
        when(sut.longLiveCacheConnector.find[SubmissionDetails](any(), any())(any())).thenReturn(Future.successful(Some(submissionDetails)))

        val result = Await.result(sut.submissionDetails("123"), 5.seconds)

        result mustBe Some(submissionDetails)
        verify(sut.longLiveCacheConnector, times(1)).find[SubmissionDetails](Matchers.eq("123"), Matchers.eq("CTUTR"))(any())
      }
    }

    "return none" when {
      "cache doesn't have data" in {
        val sut = createSUT
        when(sut.longLiveCacheConnector.find[SubmissionDetails](any(), any())(any())).thenReturn(Future.successful(None))

        val result = Await.result(sut.submissionDetails("123"), 5.seconds)

        result mustBe None
      }
    }
  }

  "updateSubmissionDetails" should {
    "save the data in cache" in {
      val sut = createSUT
      val submissionDetails = SubmissionDetails(pdfUploaded = false, metadataUploaded = false)
      when(sut.longLiveCacheConnector.createOrUpdate[SubmissionDetails](any(), any(), any())(any())).thenReturn(Future.successful(submissionDetails))

      val result = Await.result(sut.updateSubmissionDetails("123", submissionDetails), 5.seconds)

      result mustBe submissionDetails
      verify(sut.longLiveCacheConnector, times(1)).createOrUpdate(any(), Matchers.eq(submissionDetails), any())(any())

    }
  }

  "removeSubmissionDetails" should {
    "remove the details from cache" in {
      val sut = createSUT
      when(sut.longLiveCacheConnector.removeById(any())).thenReturn(Future.successful(true))

      val result = Await.result(sut.removeSubmissionDetails("123"), 5.seconds)

      result mustBe true
    }
  }

  val mockLongLiveCacheConnector = mock[LongLiveCacheConnector]

  def createSUT = new SUT
  class SUT extends SubmissionRepository(mockLongLiveCacheConnector)

}
