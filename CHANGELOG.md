
## v3.0.0 - 2017-02-06
  * Security is now done via JSON Web Tokens (JWT) - details here https://jwt.io/introduction/
  * DH is no longer a monolith application - it has been refactored into a number of microservices. This allows for better horizontal scalability and flexibility in deployment options
  * Added support for the Riak database (we used to only support PostgreSQL)
