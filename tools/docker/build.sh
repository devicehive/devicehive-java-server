#!/bin/bash

function helpMessage() {
    echo "To build/update docker image with current version of DeviceHive Java server"
    echo "and admin console you need to place following files in this directory:"
    echo "DeviceHive Java server:"
    echo "======================="
    echo "DeviceHive-current.war"
    echo "dh_dbtool-current.jar"
    echo ""
    echo "DeviceHive admin console"
    echo "========================"
    echo "devicehive-admin-console.tar"
    echo ""
    echo "See https://github.com/devicehive/devicehive-java for java server build instructions"
    echo "See https://github.com/devicehive/devicehive-admin-console for admin console"
    echo ""
}

if [[ `id -u` != "0" ]]; then
    echo "Error: You must be root in order to build docker image"
    helpMessage
    exit 1;
fi

cd `dirname $0`

source defs.sh

if [ ! -e DeviceHive-current.war ] || [ ! -e dh_dbtool-current.jar ] || [ ! -e devicehive-admin-console.tar ]; then
    helpMessage
    exit 1
fi

echo "Build"
docker build -t ${DH_DOCKER_NAME}:${DH_VERSION} .

