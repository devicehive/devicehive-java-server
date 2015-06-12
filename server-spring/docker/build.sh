#!/usr/bin/env bash
source defs.sh

docker build -t ${DH_DOCKER_NAME}:${DH_VERSION} .
