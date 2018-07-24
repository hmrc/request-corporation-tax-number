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

package services

import akka.actor.ActorSystem
import connectors.FileUploadConnector
import model.{Envelope, File}
import model.domain.MimeContentType
import org.mockito.Matchers.any
import org.mockito.{Matchers, Mockito}
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class FileUploadServiceSpec extends PlaySpec with MockitoSugar {

  "FileUploadService" must {

    "able to create enveloper" in {
      val sut = createSUT
      when(sut.fileUploadConnector.createEnvelope(any())).thenReturn(Future.successful("123"))

      val envelopeId = Await.result(sut.createEnvelope(), 5.seconds)

      envelopeId mustBe "123"
    }


    "able to upload the file" in {
      val sut = createSUT
      when(sut.fileUploadConnector.uploadFile(any(), any(), any(), any(), any())(any())).thenReturn(Future.successful(HttpResponse(200)))

      val result = Await.result(sut.uploadFile(new Array[Byte](1), "123", fileName, contentType), 5.seconds)

      result.status mustBe 200
      Mockito.verify(sut.fileUploadConnector, Mockito.times(1)).uploadFile(any(), Matchers.eq(s"$fileName"),
        Matchers.eq(contentType), any(), Matchers.eq(s"$fileId"))(any())
    }

    "able to close the envelope" in {
      val sut = createSUT
      when(sut.fileUploadConnector.closeEnvelope(any())(any())).thenReturn(Future.successful("123"))

      val result = Await.result(sut.closeEnvelope("123"), 5.seconds)

      result mustBe "123"
    }

    "able to get the envelope" in {
      val sut = createSUT
      when(sut.fileUploadConnector.envelopeSummary("123")).thenReturn(Future.successful(Envelope("123",Some("callback"),"OPEN",Some(Seq(File("pdf","open"))))))

      val result = Await.result(sut.envelopeSummary("123"), 5.seconds)

      result mustBe Envelope("123",Some("callback"),"OPEN",Some(Seq(File("pdf","open"))))
    }
  }

  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private implicit val as: ActorSystem = ActorSystem()

  private val fileName = "CTUTR.pdf"
  private val fileId = "CTUTR"
  private val contentType = MimeContentType.ApplicationPdf

  def createSUT = new SUT

  val mockFileUploadConnector = mock[FileUploadConnector]

  class SUT extends FileUploadService(mockFileUploadConnector)

}
