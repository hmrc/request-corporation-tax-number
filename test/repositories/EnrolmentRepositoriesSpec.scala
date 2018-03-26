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
import model.IFormDetails
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.SessionId
import scala.concurrent.duration._

import scala.concurrent.{Await, Future}

class EnrolmentRepositoriesSpec extends PlaySpec with MockitoSugar {

  private implicit val hc = HeaderCarrier(sessionId = Some(SessionId("TESTING")))

  "iFormDetails" should {
    "return IFormDetails" when {
      "cache returns data" in {
        val sut = createSUT
        val iFormDetails = IFormDetails(pdfUploaded = false, metadataUploaded = false)
        when(sut.longLiveCacheConnector.find[IFormDetails](any(), any())(any())).thenReturn(Future.successful(Some(iFormDetails)))

        val result = Await.result(sut.iFormDetails("123"), 5.seconds)

        result mustBe Some(iFormDetails)
        verify(sut.longLiveCacheConnector, times(1)).find[IFormDetails](Matchers.eq("123"), Matchers.eq("IForms"))(any())
      }
    }

    "return none" when {
      "cache doesn't have data" in {
        val sut = createSUT
        when(sut.longLiveCacheConnector.find[IFormDetails](any(), any())(any())).thenReturn(Future.successful(None))

        val result = Await.result(sut.iFormDetails("123"), 5.seconds)

        result mustBe None
      }
    }
  }

  "updateIFormDetails" should {
    "save the data in cache" in {
      val sut = createSUT
      val iFormDetails = IFormDetails(pdfUploaded = false, metadataUploaded = false)
      when(sut.longLiveCacheConnector.createOrUpdate[IFormDetails](any(), any(), any())(any())).thenReturn(Future.successful(iFormDetails))

      val result = Await.result(sut.updateIFormDetails("123", iFormDetails), 5.seconds)

      result mustBe iFormDetails
      verify(sut.longLiveCacheConnector, times(1)).createOrUpdate(any(), Matchers.eq(iFormDetails), any())(any())

    }
  }

  "remove IForm details" should {
    "remove the details from cache" in {
      val sut = createSUT
      when(sut.longLiveCacheConnector.removeById(any())).thenReturn(Future.successful(true))

      val result = Await.result(sut.removeIFormDetails("123"), 5.seconds)

      result mustBe true
    }
  }

  val mockLongLiveCacheConnector = mock[LongLiveCacheConnector]

  def createSUT = new SUT
  class SUT extends EnrolmentRepository(mockLongLiveCacheConnector)

}
