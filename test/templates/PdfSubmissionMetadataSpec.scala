/*
 * Copyright 2024 HM Revenue & Customs
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

package templates

import helper.TestFixture
import model.templates.CTUTRMetadata
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import play.twirl.api.Xml

class PdfSubmissionMetadataSpec extends TestFixture { // scalastyle:off magic.number

  val pdfSubmission = CTUTRMetadata(appConfig)
  val pdfSubmissionMetadata: Xml = templates.xml.pdfSubmissionMetadata(pdfSubmission)

  "pdfSubmissionMetadata" should {

    "not have a line feed character at the top of the file" when {

      "the xml is generated" in  {
        val generatedXml = pdfSubmissionMetadata.toString()

        generatedXml(0) mustNot be('\n')
      }
    }

    "populate the correct header details" when {

      val doc = Jsoup.parse(pdfSubmissionMetadata.toString(), "", Parser.xmlParser)

      "the pdf submission xml is generated" in {
        doc.select("header > title").text() mustBe pdfSubmission.submissionReference
        doc.select("header > format").text() mustBe pdfSubmission.fileFormat
        doc.select("header > mime_type").text() mustBe pdfSubmission.mimeType
        doc.select("header > store").text() mustBe pdfSubmission.store.toString
        doc.select("header > source").text() mustBe pdfSubmission.source
        doc.select("header > target").text() mustBe pdfSubmission.target
        doc.select("header > reconciliation_id").text() mustBe pdfSubmission.reconciliationId
      }

      "the reconciliation_id must be in the correct format" in {
        doc.select("header > reconciliation_id").text().matches("(^[A-Z0-9]{3}-[A-Z0-9]{4}-[A-Z0-9]{3}-[0-9]{14}$)") mustBe true
      }

    }

    "populate the correct attribute details for the hmrc_time_of_receipt attribute" when {

      "the pdf submission xml is generated" in {

        val doc = Jsoup.parse(pdfSubmissionMetadata.toString(), "", Parser.xmlParser)

        val section = doc.select("metadata > attribute").get(0)

        section.select("attribute_name").text() mustBe "hmrc_time_of_receipt"
        section.select("attribute_type").text() mustBe "time"
        section.select("attribute_value").text() mustBe pdfSubmission.hmrcReceivedAt
        section.select("attribute_value").text() mustMatchDateTimeFormat "dd/MM/yyyy HH:mm:ss"
      }
    }

    "populate the correct attribute details for the time_xml_created attribute" when {

      "the pdf submission xml is generated" in {
        val doc = Jsoup.parse(pdfSubmissionMetadata.toString(), "", Parser.xmlParser)

        val section = doc.select("metadata > attribute").get(1)

        section.select("attribute_name").text() mustBe "time_xml_created"
        section.select("attribute_type").text() mustBe "time"
        section.select("attribute_value").text() mustBe pdfSubmission.xmlCreatedAt
        section.select("attribute_value").text() mustMatchDateTimeFormat "dd/MM/yyyy HH:mm:ss"
      }
    }

    "populate the correct attribute details for the submission_reference attribute" when {

      "the pdf submission xml is generated" in {
        val doc = Jsoup.parse(pdfSubmissionMetadata.toString(), "", Parser.xmlParser)

        val section = doc.select("metadata > attribute").get(2)

        section.select("attribute_name").text()  mustBe "submission_reference"
        section.select("attribute_type").text()  mustBe "string"
        section.select("attribute_value").text.matches("([A-Z0-9]{3}-[A-Z0-9]{4}-[A-Z0-9]{3})")  mustBe true
      }
    }

    "populate the correct attribute details for the form_id attribute" when {

      "the pdf submission xml is generated" in {
        val doc = Jsoup.parse(pdfSubmissionMetadata.toString(), "", Parser.xmlParser)

        val section = doc.select("metadata > attribute").get(3)

        section.select("attribute_name").text()  mustBe "form_id"
        section.select("attribute_type").text()  mustBe "string"
        section.select("attribute_value").text()  mustBe pdfSubmission.formId
      }
    }

    "populate the correct attribute details for the number_pages attribute" when {

      "the pdf submission xml is generated" in {
        val doc = Jsoup.parse(pdfSubmissionMetadata.toString(), "", Parser.xmlParser)

        val section = doc.select("metadata > attribute").get(4)

        section.select("attribute_name").text()  mustBe "number_pages"
        section.select("attribute_type").text()  mustBe "integer"
        section.select("attribute_value").text()  mustBe pdfSubmission.numberOfPages.toString
      }
    }

    "populate the correct attribute details for the source attribute" when {

      "the pdf submission xml is generated" in {
        val doc = Jsoup.parse(pdfSubmissionMetadata.toString(), "", Parser.xmlParser)

        val section = doc.select("metadata > attribute").get(5)

        section.select("attribute_name").text()  mustBe "source"
        section.select("attribute_type").text()  mustBe "string"
        section.select("attribute_value").text()  mustBe pdfSubmission.source
      }
    }

    "populate the correct attribute details for the customer_id attribute" when {

      "the pdf submission xml is generated" in {
        val doc = Jsoup.parse(pdfSubmissionMetadata.toString(), "", Parser.xmlParser)

        val section = doc.select("metadata > attribute").get(6)

        section.select("attribute_name").text()  mustBe "customer_id"
        section.select("attribute_type").text()  mustBe "string"
        section.select("attribute_value").text()  mustBe pdfSubmission.customerId
      }
    }

    "populate the correct attribute details for the submission_mark attribute" when {

      "the pdf submission xml is generated" in {
        val doc = Jsoup.parse(pdfSubmissionMetadata.toString(), "", Parser.xmlParser)

        val section = doc.select("metadata > attribute").get(7)

        section.select("attribute_name").text()  mustBe "submission_mark"
        section.select("attribute_type").text()  mustBe "string"
        section.select("attribute_value").text()  mustBe pdfSubmission.submissionMark
      }
    }

    "populate the correct attribute details for the cas_key attribute" when {

      "the pdf submission xml is generated" in {
        val doc = Jsoup.parse(pdfSubmissionMetadata.toString(), "", Parser.xmlParser)

        val section = doc.select("metadata > attribute").get(8)

        section.select("attribute_name").text()  mustBe "cas_key"
        section.select("attribute_type").text()  mustBe "string"
        section.select("attribute_value").text()  mustBe pdfSubmission.casKey
      }
    }

    "populate the correct attribute details for the classification_type attribute" when {

      "the pdf submission xml is generated" in {
        val doc = Jsoup.parse(pdfSubmissionMetadata.toString(), "", Parser.xmlParser)

        val section = doc.select("metadata > attribute").get(9)

        section.select("attribute_name").text()  mustBe "classification_type"
        section.select("attribute_type").text()  mustBe "string"
        section.select("attribute_value").text()  mustBe pdfSubmission.classificationType
      }
    }

    "populate the correct attribute details for the business_area attribute" when {

      "the pdf submission xml is generated" in {
        val doc = Jsoup.parse(pdfSubmissionMetadata.toString(), "", Parser.xmlParser)

        val section = doc.select("metadata > attribute").get(10)

        section.select("attribute_name").text()  mustBe "business_area"
        section.select("attribute_type").text()  mustBe "string"
        section.select("attribute_value").text()  mustBe pdfSubmission.businessArea
      }
    }

    "populate the correct attribute details for the attachment_count attribute" when {

      "the pdf submission xml is generated" in {
        val doc = Jsoup.parse(pdfSubmissionMetadata.toString(), "", Parser.xmlParser)

        val section = doc.select("metadata > attribute").get(11)

        section.select("attribute_name").text()  mustBe "attachment_count"
        section.select("attribute_type").text()  mustBe "int"
        section.select("attribute_value").text()  mustBe pdfSubmission.attachmentCount.toString
      }
    }
  }


}
