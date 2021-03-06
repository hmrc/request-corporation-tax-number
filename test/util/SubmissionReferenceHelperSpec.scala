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

package util

import org.scalatest.{Matchers, OptionValues, WordSpecLike}
import utils.SubmissionReferenceHelper.createSubmissionRef

class SubmissionReferenceHelperSpec extends WordSpecLike with Matchers with OptionValues {

  "createSubmissionRef" should {
    "create submission reference of default length" in {
      val submissionRef = createSubmissionRef()
      submissionRef.length shouldBe 12
      submissionRef.contains("-") shouldBe true

      submissionRef.substring(0,
        submissionRef.indexOf("-")).length shouldBe 3
      submissionRef.substring(0,3).contains("-") shouldBe false
      submissionRef.substring(4,8).contains("-") shouldBe false
      submissionRef.substring(9,12).contains("-") shouldBe false
      submissionRef.matches("([A-Z0-9]{3}-[A-Z0-9]{4}-[A-Z0-9]{3})") shouldBe true
    }

    "create submission reference of non-default length" in {
      val submissionRef = createSubmissionRef(12)
      submissionRef.length shouldBe 14
      submissionRef.contains("-") shouldBe true
    }
  }
}