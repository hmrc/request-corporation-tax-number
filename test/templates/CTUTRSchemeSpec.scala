/*
 * Copyright 2026 HM Revenue & Customs
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
import model.{CompanyDetails, Submission}
import model.templates.{CTUTRMetadata, SubmissionViewModel}
import templates.html.CTUTRScheme

class CTUTRSchemeSpec extends TestFixture {

  private val viewModel: SubmissionViewModel = SubmissionViewModel(
    Submission(CompanyDetails("Big Company", "AB123123")),
    CTUTRMetadata(appConfig)
  )

  private val rendered = CTUTRScheme(viewModel)

  "CTUTRScheme" should {

    "expose render, f and ref companion methods" when {

      "render is called" in {
        CTUTRScheme.render(viewModel) mustBe rendered
      }

      "f is called" in {
        CTUTRScheme.f(viewModel) mustBe rendered
      }

      "ref is accessed" in {
        CTUTRScheme.ref mustBe CTUTRScheme
      }
    }
  }

}
