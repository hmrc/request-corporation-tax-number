package model

case class EnvelopeSummaryReponse(envelopeId: String, callbackUrl: String, status: String, files: Seq[File])