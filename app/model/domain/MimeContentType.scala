package model.domain

sealed trait MimeContentType {
  def description: String
}

object MimeContentType {
  case object ApplicationPdf extends MimeContentType { val description = "application/pdf" }
  case object ApplicationXml extends MimeContentType { val description = "application/xml" }
}