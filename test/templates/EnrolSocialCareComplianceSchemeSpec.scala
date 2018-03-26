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

package templates

import model.templates.{EnrolmentViewModel, _}
import org.jsoup.Jsoup
import org.scalatest.Matchers
import play.twirl.api.Html
import uk.gov.hmrc.play.test.UnitSpec

class EnrolSocialCareComplianceSchemeSpec extends UnitSpec with Matchers {

  "EnrolSocialCareEmploymentForm" should {

    "display the correct sections of the social care enrolment form" when {

      "the html document is created with all data" in {

        val sut = createSUT(enrollDirectAllViewModel)

        val doc = Jsoup.parse(sut.toString())

        doc.select("h1").text() shouldBe "Apply for the National Minimum Wage social care compliance scheme"
        doc.getElementById("timestamp").text() shouldBe "Submitted on Tuesday 31 October 2017 at 15:18:12"
        doc.getElementById("capacity-heading").text() shouldBe "Capacity Registered"
        doc.getElementById("applicant-contact-details-heading").text() shouldBe "Agent contact details"
        doc.getElementById("employee-details-heading").text() shouldBe "Employer contact details"
      }

      "the html document is created with only capacity registered" in {

        val sut = createSUT(enrollDirectApplicantViewModel)

        val doc = Jsoup.parse(sut.toString())

        doc.select("h1").text() shouldBe "Apply for the National Minimum Wage social care compliance scheme"
        doc.getElementById("capacity-heading").text() shouldBe "Capacity Registered"
        doc.toString should not contain "Agent contact details"
        doc.toString should not contain "Employer contact details"
      }

      "the html document is created with only employer details and capacity" in {

        val sut = createSUT(enrollDirectApplicantWithEmployeeViewModel)

        val doc = Jsoup.parse(sut.toString())

        doc.select("h1").text() shouldBe "Apply for the National Minimum Wage social care compliance scheme"
        doc.getElementById("capacity-heading").text() shouldBe "Capacity Registered"
        doc.toString should not contain "Agent contact details"
        doc.getElementById("employee-details-heading").text() shouldBe "Employer contact details"
      }

    }

    "display the 'Capacity Registered' section of the enrolment form" when {

      "an enrolmentViewModel is provided with applicant's details" in {

        val sut = createSUT(enrollDirectAllViewModel)

        val doc = Jsoup.parse(sut.toString())

        val capacity = doc.select("table:nth-of-type(1) > tbody > tr:nth-of-type(1)")

        capacity.select("td:nth-of-type(1)").text() shouldBe "Capacity registered as"
        capacity.select("td:nth-of-type(2)").text() shouldBe "Individual"
      }

    }

    "display the 'Agent contact details' section of the enrolment form" when {

      "an enrolmentViewModel is provided with applicant's contact details" in {
        val sut = createSUT(enrollDirectAllViewModel)

        val doc = Jsoup.parse(sut.toString())

        val name = doc.getElementById("applicant__contact-details").select("tbody > tr:nth-of-type(1)")

        val address = doc.getElementById("applicant__contact-details").select("tbody > tr:nth-of-type(2)")

        val telephone = doc.getElementById("applicant__contact-details").select("tbody > tr:nth-of-type(3)")

        val email = doc.getElementById("applicant__contact-details").select("tbody > tr:nth-of-type(4)")

        name.select("td:nth-of-type(1)").text() shouldBe "Name"
        name.select("td:nth-of-type(2)").text() shouldBe "John Smith"

        address.select("td:nth-of-type(1)").text() shouldBe "Address"
        address.select("td:nth-of-type(2)").text() shouldBe "1 Test street 2 Test street 3 Test street 4 Test street 5 Test street DH9 9PB"

        telephone.select("td:nth-of-type(1)").text() shouldBe "Telephone number"
        telephone.select("td:nth-of-type(2)").text() shouldBe "0191 2222222"

        email.select("td:nth-of-type(1)").text() shouldBe "Email address"
        email.select("td:nth-of-type(2)").text() shouldBe "test@test.com"
      }

    }

    "display the 'Employer contact details' section of the enrolment form" when {

      "an enrolmentViewModel is provided with employees's contact details" in {
        val sut = createSUT(enrollDirectAllViewModel)

        val doc = Jsoup.parse(sut.toString())

        val name = doc.getElementById("employee__contact-details").select("tbody > tr:nth-of-type(1)")
        val address = doc.getElementById("employee__contact-details").select("tbody > tr:nth-of-type(2)")
        val telephone = doc.getElementById("employee__contact-details").select("tbody > tr:nth-of-type(3)")
        val email = doc.getElementById("employee__contact-details").select("tbody > tr:nth-of-type(4)")
        val utr = doc.getElementById("employee__contact-details").select("tbody > tr:nth-of-type(5)")
        val paye = doc.getElementById("employee__contact-details").select("tbody > tr:nth-of-type(6)")

        name.select("td:nth-of-type(1)").text() shouldBe "Name"
        name.select("td:nth-of-type(2)").text() shouldBe "HMRC"

        address.select("td:nth-of-type(1)").text() shouldBe "Address"
        address.select("td:nth-of-type(2)").text() shouldBe "1 Test street 2 Test street 3 Test street 4 Test street 5 Test street DH9 9PB"

        telephone.select("td:nth-of-type(1)").text() shouldBe "Telephone number"
        telephone.select("td:nth-of-type(2)").text() shouldBe "0191 3333333"

        email.select("td:nth-of-type(1)").text() shouldBe "Email address"
        email.select("td:nth-of-type(2)").text() shouldBe "test@company.com"

        utr.select("td:nth-of-type(1)").text() shouldBe "Unique Tax Reference"
        utr.select("td:nth-of-type(2)").text() shouldBe "12345678"

        paye.select("td:nth-of-type(1)").text() shouldBe "PAYE Reference"
        paye.select("td:nth-of-type(2)").text() shouldBe "1234567890"
      }

      "an enrolmentViewModel is provided with employees's minimum data" in {
        val sut = createSUT(enrollDirectMinimumViewModel)

        val doc = Jsoup.parse(sut.toString())

        val name = doc.getElementById("employee__contact-details").select("tbody > tr:nth-of-type(1)")
        val address = doc.getElementById("employee__contact-details").select("tbody > tr:nth-of-type(2)")
        val telephone = doc.getElementById("employee__contact-details").select("tbody > tr:nth-of-type(3)")
        val email = doc.getElementById("employee__contact-details").select("tbody > tr:nth-of-type(4)")
        val utr = doc.getElementById("employee__contact-details").select("tbody > tr:nth-of-type(5)")
        val paye = doc.getElementById("employee__contact-details").select("tbody > tr:nth-of-type(6)")

        name.select("td:nth-of-type(1)").text() shouldBe "Name"
        name.select("td:nth-of-type(2)").text() shouldBe "HMRC"

        address.select("td:nth-of-type(1)").text() shouldBe "Address"
        address.select("td:nth-of-type(2)").text() shouldBe "1 Test street 2 Test street 3 Test street 4 Test street 5 Test street DH9 9PB"

        telephone.select("td:nth-of-type(1)").text() shouldBe empty
        telephone.select("td:nth-of-type(2)").text() shouldBe empty

        email.select("td:nth-of-type(1)").text() shouldBe empty
        email.select("td:nth-of-type(2)").text() shouldBe empty

        utr.select("td:nth-of-type(1)").text() shouldBe empty
        utr.select("td:nth-of-type(2)").text() shouldBe empty

        paye.select("td:nth-of-type(1)").text() shouldBe empty
        paye.select("td:nth-of-type(2)").text() shouldBe empty
      }

    }

  }

  private val enrollDirectAllViewModel: EnrolmentViewModel = EnrolmentViewModel(
    capacityRegistering = "Individual",
    timestamp = "Tuesday 31 October 2017 at 15:18:12",
    agent = Some(Agent(
      name = "John Smith",
      address = Some(Address(
        "1 Test street",
        "2 Test street",
        "3 Test street",
        "4 Test street",
        "5 Test street",
        "DH9 9PB"
      )),
      telephoneNumber = Some("0191 2222222"),
      emailAddress = Some("test@test.com")
    )),
    employer = Some(Employer(
      name = "HMRC",
      address = Some(Address(
        "1 Test street",
        "2 Test street",
        "3 Test street",
        "4 Test street",
        "5 Test street",
        "DH9 9PB"
      )),
      telephoneNumber = Some("0191 3333333"),
      emailAddress = Some("test@company.com"),
      taxpayerReference = Some("12345678"),
      payeReference = Some("1234567890")
    ))
  )

  private val enrollDirectMinimumViewModel: EnrolmentViewModel = EnrolmentViewModel(
    capacityRegistering = "Individual",
    timestamp = "Tuesday 31 October 2017 at 15:18:12",
    agent = Some(Agent(
      name = "John Smith",
      address = Some(Address(
        "1 Test street",
        "2 Test street",
        "3 Test street",
        "4 Test street",
        "5 Test street",
        "DH9 9PB"
      )),
      telephoneNumber = Some("0191 2222222"),
      emailAddress = Some("test@test.com")
    )),
    employer = Some(Employer(
      name = "HMRC",
      address = Some(Address(
        "1 Test street",
        "2 Test street",
        "3 Test street",
        "4 Test street",
        "5 Test street",
        "DH9 9PB"
      )),
      telephoneNumber = None,
      emailAddress = None,
      taxpayerReference = None,
      payeReference = None
    ))
  )

  private val enrollDirectApplicantViewModel: EnrolmentViewModel = EnrolmentViewModel(
    capacityRegistering = "Individual",
    timestamp = "Tuesday 31 October 2017 at 15:18:12",
    agent = Some(Agent(
      name = "John Smith",
      address = Some(Address(
        "1 Test street",
        "2 Test street",
        "3 Test street",
        "4 Test street",
        "5 Test street",
        "DH9 9PB"
      )),
      telephoneNumber = None,
      emailAddress = None
    )),
    employer = None
  )

  private val enrollDirectApplicantWithEmployeeViewModel: EnrolmentViewModel = EnrolmentViewModel(
    capacityRegistering = "Individual",
    timestamp = "Tuesday 31 October 2017 at 15:18:12",
    agent = None,
    employer = Some(Employer(
      name = "HMRC",
      address = Some(Address(
        "1 Test street",
        "2 Test street",
        "3 Test street",
        "4 Test street",
        "5 Test street",
        "DH9 9PB"
      )),
      telephoneNumber = Some("0191 3333333"),
      emailAddress = Some("test@company.com"),
      taxpayerReference = Some("12345678"),
      payeReference = Some("1234567890")
    ))
  )

  private def createSUT(viewModel: EnrolmentViewModel): Html = templates.html.EnrolSocialCareComplianceScheme(viewModel)
}
