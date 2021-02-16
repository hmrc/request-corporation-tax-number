/*
 * Copyright 2021 HM Revenue & Customs
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

import helper.TestFixture
import model.domain.MimeContentType
import model.{Envelope, File}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class FileUploadServiceSpec extends TestFixture {

  private val fileName = "CTUTR.pdf"
  private val fileId = "CTUTR"
  private val contentType = MimeContentType.ApplicationPdf

  val fileUploadService: FileUploadService = new FileUploadService(mockFileUploadConnector)

  "FileUploadService" must {

    "able to create enveloper" in {
      when(fileUploadService.fileUploadConnector.createEnvelope(any())).thenReturn(Future.successful("123"))

      val envelopeId = Await.result(fileUploadService.createEnvelope(), 5.seconds)

      envelopeId mustBe "123"
    }


    "able to upload the file" in {
      when(fileUploadService.fileUploadConnector.uploadFile(any(), eqTo(fileName), eqTo(contentType), any(), eqTo(fileId))(any()))
        .thenReturn(Future.successful(HttpResponse(200, "")))

      val result = Await.result(fileUploadService.uploadFile(new Array[Byte](1), "123", fileName, contentType), 5.seconds)

      result.status mustBe 200
    }

    "able to close the envelope" in {
      when(fileUploadService.fileUploadConnector.closeEnvelope(any())(any())).thenReturn(Future.successful("123"))

      val result = Await.result(fileUploadService.closeEnvelope("123"), 5.seconds)

      result mustBe "123"
    }

    "able to get the envelope" in {
      when(fileUploadService.fileUploadConnector.envelopeSummary("123")).thenReturn(Future.successful(Envelope("123",Some("callback"),"OPEN",Some(Seq(File("pdf","open"))))))

      val result = Await.result(fileUploadService.envelopeSummary("123"), 5.seconds)

      result mustBe Envelope("123",Some("callback"),"OPEN",Some(Seq(File("pdf","open"))))
    }
  }



}
