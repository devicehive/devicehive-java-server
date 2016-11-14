This document tracks changes to the API between versions.

# 3.0.0

## RESTful API
* Removed OAuth server (permanently deleted `/oauth/client`, `/user/{userId}/oauth/grant` and `/oauth2/token` endpoints)
* OAuth providers (Github, Facebook, Google, etc) now support JWT instead of Access Key (`/auth/accesskey` endpoint renamed to `/oauth/token` and it returns JWT)

## WebSocket API
* Added JWT authentication support (`accessKey` request parameter renamed to `token`)