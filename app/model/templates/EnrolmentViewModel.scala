package model.templates

import model.{Enrolment, InternationalAddress, UkAddress}

case class EnrolmentViewModel(capacityRegistering: String,
                              timestamp : String,
                              agent: Option[Agent],
                              employer: Option[Employer])

case class Employer(name: String,
                    address: Option[Address],
                    telephoneNumber: Option[String],
                    emailAddress: Option[String],
                    taxpayerReference: Option[String],
                    payeReference: Option[String])

case class Agent(name: String,
                 address: Option[Address],
                 telephoneNumber: Option[String],
                 emailAddress: Option[String])

case class Address(line1: String,
                   line2: String,
                   line3: String,
                   line4: String,
                   line5: String,
                   line6: String)

object EnrolmentViewModel {

  private def modelAddress(address : (Option[UkAddress], Option[InternationalAddress])) : Option[Address] = {
    val d: Option[Address] = address match {
      case (Some(_), Some(_)) =>
        throw new IllegalArgumentException("Cannot have a UK and International address")
      case (Some(uk), None) =>
        Some(Address(
          uk.addressLine1,
          uk.addressLine2,
          uk.addressLine3.getOrElse(""),
          uk.addressLine4.getOrElse(""),
          uk.addressLine5.getOrElse(""),
          uk.postcode
        ))
      case (None, Some(int)) =>
        Some(Address(
          int.addressLine1,
          int.addressLine2,
          int.addressLine3.getOrElse(""),
          int.addressLine4.getOrElse(""),
          int.addressLine5.getOrElse(""),
          int.country
        ))
      case _ => None
    }
    d
  }

  private def modelAgent(agent: Option[model.Agent]) : Option[Agent] = {
    agent match {
      case None => None
      case Some(a) =>
        Some(Agent(
          name = a.name,
          address = modelAddress((a.ukAddress, a.internationalAddress)),
          telephoneNumber = a.telephoneNumber,
          emailAddress = a.emailAddress
        ))
    }
  }

  private def modelEmployer(employer : model.Employer) : Option[Employer] = {
    Some(Employer(
      name = employer.name,
      address = modelAddress((employer.ukAddress, employer.internationalAddress)),
      telephoneNumber = employer.telephoneNumber,
      emailAddress = employer.emailAddress,
      taxpayerReference = employer.taxpayerReference,
      payeReference = employer.payeReference
    ))
  }

  def apply(enrolment : Enrolment) : EnrolmentViewModel = {

    val timestamp = s"${enrolment.time.toString("EEEE dd MMMM yyyy")} at ${enrolment.time.toString("HH:mm:ss")}"

    EnrolmentViewModel(
      capacityRegistering = enrolment.capacityRegistering,
      timestamp = timestamp,
      agent = modelAgent(enrolment.agent),
      employer = modelEmployer(enrolment.employer)
    )
  }

}