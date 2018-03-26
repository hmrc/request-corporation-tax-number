package model

import play.api.libs.json.{Format, Json}

case class IFormDetails(pdfUploaded: Boolean, metadataUploaded: Boolean)

object IFormDetails {
  implicit val formatIFormDetails: Format[IFormDetails] = Json.format[IFormDetails]
}