package repositories

import javax.inject.Inject

import com.google.inject.Singleton
import connectors.LongLiveCacheConnector
import model.IFormDetails
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

/**
  * Find, update and remove a form in mongo
  */

@Singleton
class EnrolmentRepository @Inject()(
                                   val longLiveCacheConnector: LongLiveCacheConnector
                                   ) {

  private val IFormKey = "IForms"

  def iFormDetails(id: String)(implicit hc: HeaderCarrier): Future[Option[IFormDetails]] = {
    Logger.info(s"[EnrolmentRepository][iFormDetails][searching for iForm in mongo $id")
    longLiveCacheConnector.find[IFormDetails](id, IFormKey)
  }

  def updateIFormDetails(id: String, details: IFormDetails)(implicit hc: HeaderCarrier): Future[IFormDetails] = {
    Logger.info(s"[EnrolmentRepository][updateIFormDetails][updating iForm in mongo $id")
    longLiveCacheConnector.createOrUpdate[IFormDetails](id, details, IFormKey)
  }

  def removeIFormDetails(id: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    Logger.info(s"[EnrolmentRepository][removeIFormDetails][removing iForm from mongo $id")
    longLiveCacheConnector.removeById(id)
  }

}