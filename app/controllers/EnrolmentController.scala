package controllers

import javax.inject.Inject

import com.google.inject.Singleton
import model.{Enrolment, FileUploadCallback}
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Action
import services.EnrolmentService
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class EnrolmentController @Inject()(
                                   val enrolmentService: EnrolmentService
                                   ) extends BaseController {

  def enrol() : Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      request.body.validate[Enrolment].fold(
        errors => {
          Logger.warn(s"[EnrolmentController][enrol] Bad Request] $errors")
          Future.successful(BadRequest("invalid payload provided"))
        },
        e => {
            Logger.info(s"[EnrolmentController][enrol] processing enrolment")
            enrolmentService.enrol(e) map {
              response =>
                Logger.info(s"[EnrolmentController][enrol] processed enrolment $response")
                Ok(Json.toJson(response))
            } recoverWith {
              case e : Exception =>
                Logger.error(s"[EnrolmentController][enrol][exception returned when processing enrolment] ${e.getMessage}")
                Future.successful(InternalServerError)
            }
          }
      )
  }

  def fileUploadCallback(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[FileUploadCallback] {
      fileUploadCallback =>
        Logger.info(s"[EnrolmentController][fileUploadCallback] processing callback $fileUploadCallback")
        enrolmentService.fileUploadCallback(fileUploadCallback) map {
          _ =>
            Ok
        } recoverWith {
          case _ : Exception =>
            Future.successful(InternalServerError)
        }
    }
  }

}