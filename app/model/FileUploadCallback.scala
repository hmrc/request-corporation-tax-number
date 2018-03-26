package model

import play.api.libs.json.{Format, Json}

/**
  * TODO implement toMap method when auditing to splunk
  */
case class FileUploadCallback(envelopeId: String, fileId: String, status: String, reason: Option[String]) {
//  def toMap: Map[String, String] = Map(
//    "envelopId" -> envelopeId,
//    "status" -> status,
//    "reason" -> reason.getOrElse(""),
//    "file-Id" -> fileId
//  )
}

object FileUploadCallback {
  implicit val formatFileUpload: Format[FileUploadCallback] = Json.format[FileUploadCallback]
}


