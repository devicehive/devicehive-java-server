This document tracks changes to the API between versions.

# 3.0.0

## RESTful API
* Removed OAuth server (permanently deleted `/oauth/client`, `/user/{userId}/oauth/grant` and `/oauth2/token` endpoints)
* OAuth providers (Github, Facebook, Google, etc) now support JWT instead of Access Key (`/auth/accesskey` endpoint renamed to `/oauth/token` and it returns JWT)
* Added JWT authentication support:
    - `/token` endpoint for JWT access and refresh token request with JWT payload;
    - `/token/refresh` endpoint for JWT access token request with refresh token. 
    
    See [DeviceHive API Swagger](http://playground.devicehive.com/api/swagger?url=http%3A%2F%2Fplayground.devicehive.com%2Fapi%2Frest%2Fswagger.json) for more details.  
    

## WebSocket API
* Added JWT authentication support (`accessKey` request parameter renamed to `token`)
* Added `token/refresh` action (takes `refreshToken` parameter and responses new access token in `accessToken` parameter)