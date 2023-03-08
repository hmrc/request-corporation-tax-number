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

package utils

object SubmissionReferenceHelper {
  def createSubmissionRef(length: Integer = 10): String = {
    val len: Integer = {
      if (length < 10) 10 else if (length % 2 == 1) length + 1  else length
    }
    val first: Integer = (len - 4) / 2

    val chars = ('A' to 'Z') ++ ('0' to '9')
    val sb = new StringBuilder
    for (i <- 1 to length) {
      val randomNum = util.Random.nextInt(chars.toList.size)
      sb.append(chars.toList(randomNum))
    }
    sb.toString.substring(0,first) + "-" +
      sb.toString.substring(first,(first+4)) + "-" +
      sb.toString.substring((first+4),length)
  }
}
