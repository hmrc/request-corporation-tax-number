package connectors

import config.MicroserviceAppConfig
import model.{Employer, Enrolment}
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when, _}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEachTestData, TestData}
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json.Json
import reactivemongo.api.commands.{DefaultWriteResult, WriteError}
import uk.gov.hmrc.cache.model.{Cache, Id}
import uk.gov.hmrc.cache.repository.CacheRepository
import uk.gov.hmrc.mongo.DatabaseUpdate

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class LongLiveCacheConnectorSpec extends PlaySpec with MockitoSugar with OneAppPerSuite with BeforeAndAfterEachTestData {

  override protected def beforeEach(testData: TestData): Unit = {
    reset(mockRepository)
  }

  "Cache Connector" should {

    "save the data in cache" when {

      "provided with string data" in {
        val sut = createSUT
        when(sut.cacheRepository.repo.createOrUpdate(any(), any(), any())).thenReturn(databaseUpdate)

        val data = Await.result(sut.createOrUpdate(id = "", data = "DATA", key = ""), atMost)

        data mustBe "DATA"
      }

      "provided with int data" in {
        val sut = createSUT
        when(sut.cacheRepository.repo.createOrUpdate(any(), any(), any())).thenReturn(databaseUpdate)

        val data = Await.result(sut.createOrUpdate(id = "", data = 10, key = ""), atMost)

        data mustBe 10
      }

      "provided with enrolment data" ignore {
        val sut = createSUT
        when(sut.cacheRepository.repo.createOrUpdate(any(), any(), any())).thenReturn(databaseUpdate)

        val data = Await.result(sut.createOrUpdate(id = "", data = enrolment, key = ""), atMost)

        data mustBe enrolment
      }

    }

    "retrieve the data from cache" when {

      "id is present in the cache" in {
        val sut = createSUT
        val eventualSomeCache = Some(Cache(Id(id), Some(Json.toJson(Map("SCC-iForm" -> "DATA")))))
        when(sut.cacheRepository.repo.findById(any(), any())(any())).thenReturn(Future.successful(eventualSomeCache))

        val data = Await.result(sut.find[String](id), atMost)

        data mustBe Some("DATA")

        verify(sut.cacheRepository.repo, times(1)).findById(Matchers.eq(id), any())(any())
      }

      "id is not present in the cache" in {
        val sut = createSUT
        when(sut.cacheRepository.repo.findById(any(), any())(any())).thenReturn(Future.successful(None))

        val data = Await.result(sut.find[String](id), atMost)

        data mustBe None

        verify(sut.cacheRepository.repo, times(1)).findById(Matchers.eq(id), any())(any())
      }
    }

    "retrieve the enrolment from cache" when {

      "id is present in the cache" ignore {
        val sut = createSUT
        val eventualSomeCache = Some(Cache(Id(id), Some(Json.toJson(Map("SCC-iForm" -> enrolment)))))
        when(sut.cacheRepository.repo.findById(any(), any())(any())).thenReturn(Future.successful(eventualSomeCache))

        val data = Await.result(sut.find[String](id, "SCC-iForm"), atMost)

        data mustBe Some(enrolment)

        verify(sut.cacheRepository.repo, times(1)).findById(Matchers.eq(id), any())(any())
      }

      "id is present in the cache but with wrong type conversion" in {
        val sut = createSUT
        val eventualSomeCache = Some(Cache(Id(id), Some(Json.toJson(Map("SCC-iForm" -> enrolment)))))
        when(sut.cacheRepository.repo.findById(any(), any())(any())).thenReturn(Future.successful(eventualSomeCache))

        val data = Await.result(sut.find[String](id, "SCC-iForm"), atMost)

        data mustBe None

        verify(sut.cacheRepository.repo, times(1)).findById(Matchers.eq(id), any())(any())
      }
    }

    "remove the session data from cache" when {

      "remove has been called with id" in {
        val sut = createSUT
        when(sut.cacheRepository.repo.removeById(any(), any())(any())).thenReturn(Future.successful(DefaultWriteResult(ok = true, 0, Nil, None, None, None)))

        val result = Await.result(sut.removeById("ABC"), atMost)

        result mustBe true
        verify(sut.cacheRepository.repo, times(1)).removeById(Matchers.eq(Id("ABC")), any())(any())
      }
    }

    "throw the error" when {

      "failed to remove the session data" in {
        val sut = createSUT
        val writeErrors = Seq(WriteError(0, 0, "Failed"))
        val eventualWriteResult = Future.successful(DefaultWriteResult(ok = false, 0, writeErrors, None, None, Some("Failed")))
        when(sut.cacheRepository.repo.removeById(any(), any())(any())).thenReturn(eventualWriteResult)

        val ex = the[RuntimeException] thrownBy Await.result(sut.removeById("ABC"), atMost)
        ex.getMessage mustBe "Failed"
      }

      "failed to remove the session data and errorMsg is None" in {
        val sut = createSUT
        val writeErrors = Seq(WriteError(0, 0, "Failed"))
        val eventualWriteResult = Future.successful(DefaultWriteResult(ok = false, 0, writeErrors, None, None, None))
        when(sut.cacheRepository.repo.removeById(any(), any())(any())).thenReturn(eventualWriteResult)

        val ex = the[RuntimeException] thrownBy Await.result(sut.removeById("ABC"), atMost)
        ex.getMessage mustBe "Error while removing the session data"
      }
    }
  }

  private val databaseUpdate = Future.successful(mock[DatabaseUpdate[Cache]])
  private val id = "ABCD"
  private val atMost = 5 seconds

  private val enrolment: Enrolment = Enrolment(
    capacityRegistering = "Individual",
    agent = None,
    employer = Employer(
      name = "Company",
      ukAddress = None,
      internationalAddress = None,
      telephoneNumber = Some("0191 2223333"),
      emailAddress = None,
      taxpayerReference = None,
      payeReference = None
    )
  )

  def createSUT = new SUT

  val config = app.injector.instanceOf[MicroserviceAppConfig]
  val mockRepository = mock[CacheRepository]

  class MockEnrolmentCacheHelper extends EnrolmentCacheRepositoryHelper(config) {
    override def expireAfter: Long = 1L

    override val repo: CacheRepository = mockRepository
  }

  class SUT extends LongLiveCacheConnector(new MockEnrolmentCacheHelper)

}
