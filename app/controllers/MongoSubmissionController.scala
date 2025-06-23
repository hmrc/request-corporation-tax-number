/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers

import javax.inject.Inject
import model.Submission
import play.api.mvc.{Action, ControllerComponents, Request}
import services.MongoSubmissionService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

class MongoSubmissionController @Inject()(val mongoSubmissionService: MongoSubmissionService,
                                          cc: ControllerComponents
                                         ) extends BackendController(cc){

  def storeSubmission(): Action[Submission] = Action.async(parse.json[Submission]) {
    implicit request: Request[Submission] => {
      mongoSubmissionService.storeSubmission(request.body)
    }
  }

}
