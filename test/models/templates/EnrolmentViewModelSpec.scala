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

package models.templates

import model.templates.EnrolmentViewModel
import model.{Enrolment, InternationalAddress, UkAddress}
import org.joda.time.LocalDateTime
import org.joda.time.format.DateTimeFormat
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec

class EnrolmentViewModelSpec extends PlaySpec with MockitoSugar {

  val time = LocalDateTime.parse("Tuesday 31 October 2017 15:18:12", DateTimeFormat.forPattern("EEEE dd MMMM yyyy HH:mm:ss"))

  "EnrolmentViewModel" must {

    "instantiate when provided an Enrolment with minimum data" in {
      val enrolment = mock[Enrolment]

      when(enrolment.capacityRegistering).thenReturn("Individual")
      when(enrolment.agent).thenReturn(None)
      when(enrolment.employer).thenReturn(model.Employer(
        name = "Company",
        ukAddress = None,
        internationalAddress = None,
        telephoneNumber = None,
        emailAddress = None,
        taxpayerReference = None,
        payeReference = None
      ))
      when(enrolment.time).thenReturn(time)

      EnrolmentViewModel.apply(enrolment) mustBe EnrolmentViewModel(
        capacityRegistering = "Individual",
        timestamp = "Tuesday 31 October 2017 at 15:18:12",
        agent = None,
        employer = Some(model.templates.Employer(
          name = "Company",
          address = None,
          telephoneNumber = None,
          emailAddress = None,
          taxpayerReference = None,
          payeReference = None
        ))
      )
    }

    "instantiate when provided maximum uk data" in {

      val enrolment = mock[Enrolment]

      when(enrolment.capacityRegistering).thenReturn("Individual")
      when(enrolment.agent).thenReturn(Some(model.Agent(
        name = "John Smith",
        ukAddress = Some(UkAddress(
          "line1",
          "line2",
          Some("line3"),
          Some("line4"),
          Some("line5"),
          "DH97QD"
        )),
        internationalAddress = None,
        telephoneNumber = Some("0191 2223333"),
        emailAddress = Some("test@test.com")
      )))
      when(enrolment.employer).thenReturn(model.Employer(
        name = "Company",
        ukAddress = Some(UkAddress(
          "line1",
          "line2",
          Some("line3"),
          Some("line4"),
          Some("line5"),
          "DH97QD"
        )),
        internationalAddress = None,
        telephoneNumber = Some("0191 2223333"),
        emailAddress = Some("test@company.com"),
        taxpayerReference = Some("12345"),
        payeReference = Some("54321")
      ))
      when(enrolment.time).thenReturn(time)

      EnrolmentViewModel.apply(enrolment) mustBe EnrolmentViewModel(
        capacityRegistering = "Individual",
        timestamp = "Tuesday 31 October 2017 at 15:18:12",
        agent = Some(model.templates.Agent(
          name = "John Smith",
          address = Some(model.templates.Address(
            "line1",
            "line2",
            "line3",
            "line4",
            "line5",
            "DH97QD"
          )),
          telephoneNumber = Some("0191 2223333"),
          emailAddress = Some("test@test.com")
        )),
        employer = Some(model.templates.Employer(
          name = "Company",
          address = Some(model.templates.Address(
            "line1",
            "line2",
            "line3",
            "line4",
            "line5",
            "DH97QD"
          )),
          telephoneNumber = Some("0191 2223333"),
          emailAddress = Some("test@company.com"),
          taxpayerReference = Some("12345"),
          payeReference = Some("54321")
        ))
      )
    }

    "instantiate when provided maximum international data" in {
      val enrolment = mock[Enrolment]

      when(enrolment.capacityRegistering).thenReturn("Individual")
      when(enrolment.agent).thenReturn(Some(model.Agent(
        name = "John Smith",
        ukAddress = None,
        internationalAddress = Some(InternationalAddress(
          "line1",
          "line2",
          Some("line3"),
          Some("line4"),
          Some("line5"),
          "France"
        )),
        telephoneNumber = Some("0191 2223333"),
        emailAddress = Some("test@test.com")
      )))
      when(enrolment.employer).thenReturn(model.Employer(
        name = "Company",
        ukAddress = None,
        internationalAddress = Some(InternationalAddress(
          "line1",
          "line2",
          Some("line3"),
          Some("line4"),
          Some("line5"),
          "France"
        )),
        telephoneNumber = Some("0191 2223333"),
        emailAddress = Some("test@company.com"),
        taxpayerReference = Some("12345"),
        payeReference = Some("54321")
      ))
      when(enrolment.time).thenReturn(time)

      EnrolmentViewModel.apply(enrolment) mustBe EnrolmentViewModel(
        capacityRegistering = "Individual",
        timestamp = "Tuesday 31 October 2017 at 15:18:12",
        agent = Some(model.templates.Agent(
          name = "John Smith",
          address = Some(model.templates.Address(
            "line1",
            "line2",
            "line3",
            "line4",
            "line5",
            "France"
          )),
          telephoneNumber = Some("0191 2223333"),
          emailAddress = Some("test@test.com")
        )),
        employer = Some(model.templates.Employer(
          name = "Company",
          address = Some(model.templates.Address(
            "line1",
            "line2",
            "line3",
            "line4",
            "line5",
            "France"
          )),
          telephoneNumber = Some("0191 2223333"),
          emailAddress = Some("test@company.com"),
          taxpayerReference = Some("12345"),
          payeReference = Some("54321")
        ))
      )
    }

    "throw an IllegalArgumentException when provided both UK and international data" in {
      val enrolment = model.Enrolment(
        capacityRegistering = "Individual",
        agent = Some(model.Agent(
          name = "John Smith",
          ukAddress = Some(UkAddress(
            "line1",
            "line2",
            Some("line3"),
            Some("line4"),
            Some("line5"),
            "DH97QD"
          )),
          internationalAddress = Some(InternationalAddress(
            "line1",
            "line2",
            Some("line3"),
            Some("line4"),
            Some("line5"),
            "France"
          )),
          telephoneNumber = Some("0191 2223333"),
          emailAddress = Some("test@test.com")
        )),
        employer = model.Employer(
          name = "Company",
          ukAddress = Some(UkAddress(
            "line1",
            "line2",
            Some("line3"),
            Some("line4"),
            Some("line5"),
            "DH97QD"
          )),
          internationalAddress = Some(InternationalAddress(
            "line1",
            "line2",
            Some("line3"),
            Some("line4"),
            Some("line5"),
            "France"
          )),
          telephoneNumber = Some("0191 2223333"),
          emailAddress = Some("test@company.com"),
          taxpayerReference = Some("12345"),
          payeReference = Some("54321")
        )
      )

      val thrown = intercept[IllegalArgumentException] {
        EnrolmentViewModel.apply(enrolment)
      }
      thrown.getMessage mustBe "Cannot have a UK and International address"
    }

  }

}
