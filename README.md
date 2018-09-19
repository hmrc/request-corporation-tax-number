# Request corporation tax number backend

CT UTR backend microservice. 

## Info

This service is also known as Ask for a copy of your Corporation Tax UTR

### Dependencies

|Service|Link|
|-|-|
|File upload frontend|https://github.com/hmrc/file-upload-frontend|
|File upload|https://github.com/hmrc/file-upload|
|Pdf generator|https://github.com/hmrc/pdf-generator-service|

## Running the service

Service Manager: CTUTR_ALL 

|Repositories|Link|
|------------|----|
|Frontend|https://github.com/hmrc/request-corporation-tax-number-frontend|
|Stub|https://github.com/hmrc/request-corporation-tax-number-stubs|
|Performance tests|https://github.com/hmrc/request-corporation-tax-number-performance-test|

Routes
-------
Port: 9201

| *Url* | *Description* |
|-------|---------------|
| /submission | The frontend submits to this endpoint |
| /file-upload/callback | Callback endpoint for file upload |
