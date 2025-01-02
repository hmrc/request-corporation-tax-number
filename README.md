# request-corporation-tax-number

CT UTR backend microservice.

This service is also known as Ask for a copy of your Corporation Tax UTR

|Repositories|Link|
|------------|----|
|Frontend|https://github.com/hmrc/request-corporation-tax-number-frontend|
|Stub|https://github.com/hmrc/request-corporation-tax-number-stubs|
|Acceptance tests|https://github.com/hmrc/request-corporation-tax-number-journey-tests|
|Performance tests|https://github.com/hmrc/request-corporation-tax-number-performance-test|

### Dependencies

| Service        |Link|
|----------------|----|
| DMS Submission |https://github.com/hmrc/dms-submission|

## Running the service locally

You will need to have the following:

* Installed/configured [service manager 2](https://github.com/hmrc/sm2).
* Installed [MongoDB](https://www.mongodb.com/docs/manual/installation/)

The service manager profile for this service is `REQUEST_CORPORATION_TAX_NUMBER`

Use the following to run all the microservices and dependant services used for CT UTR:

`sm2 --start CTUTR_ALL`

If you want to run your local copy, then stop running the backend service via the service manager and run your local code by using the following:

```
sm2 --start CTUTR_ALL
sm2 --stop REQUEST_CORPORATION_TAX_NUMBER

cd request-corporation-tax-number
sbt run
```

### Routes

Port number is `9201` but is defaulted to that in build.sbt

| Url | Description                           |
|-------|---------------------------------------|
| /submission | The frontend submits to this endpoint |
| /dms-submission/callback | Callback endpoint for dms submission  |

## Testing the Service

This service uses [sbt-scoverage](https://github.com/scoverage/sbt-scoverage) to provide test coverage reports.

Run this script before raising a PR to ensure your code changes pass the Jenkins pipeline. This runs all the unit tests with scalastyle and checks for dependency updates:

```
./run_all_tests.sh
```

## License

This code is open source software licensed under the Apache 2.0 License.