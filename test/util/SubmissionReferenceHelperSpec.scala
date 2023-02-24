/*
 * Copyright 2023 HM Revenue & Customs
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

package util

import org.scalatest.OptionValues
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import utils.SubmissionReferenceHelper.createSubmissionRef

class SubmissionReferenceHelperSpec extends AnyWordSpec with Matchers with OptionValues {

  "createSubmissionRef" should {
    "create submission reference of default length" in {
      val submissionRef = createSubmissionRef()
      submissionRef.length mustBe 12
      submissionRef.contains("-") mustBe true

      submissionRef.substring(0,
        submissionRef.indexOf("-")).length mustBe 3
      submissionRef.substring(0,3).contains("-") mustBe false
      submissionRef.substring(4,8).contains("-") mustBe false
      submissionRef.substring(9,12).contains("-") mustBe false
      submissionRef.matches("([A-Z0-9]{3}-[A-Z0-9]{4}-[A-Z0-9]{3})") mustBe true
    }

    "create submission reference of non-default length" in {
      val submissionRef = createSubmissionRef(12)
      submissionRef.length mustBe 14
      submissionRef.contains("-") mustBe true
    }
  }
}