[![Build Status](http://54.208.99.247:8080/job/DeviceHiveBuild/badge/icon)](http://54.208.99.247:8080/job/DeviceHiveBuild/) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=flat-square)](LICENSE)

DeviceHive Java server
======================

[DeviceHive]: http://devicehive.com "DeviceHive framework"
[DataArt]: http://dataart.com "DataArt"

DeviceHive turns any connected device into the part of Internet of Things.
It provides the communication layer, control software and multi-platform
libraries to bootstrap development of smart energy, home automation, remote
sensing, telemetry, remote control and monitoring software and much more.

Connect embedded Linux using Python or C++ libraries and JSON protocol or
connect AVR, Microchip devices using lightweight C libraries and BINARY protocol.
Develop client applications using HTML5/JavaScript, iOS and Android libraries.
For solutions involving gateways, there is also gateway middleware that allows
to interface with devices connected to it. Leave communications to DeviceHive
and focus on actual product and innovation.

DeviceHive license
------------------

[DeviceHive] is developed by [DataArt] Apps and distributed under Open Source
[Apache 2.0](https://en.wikipedia.org/wiki/Apache_License). This basically means
you can do whatever you want with the software as long as the copyright notice
is included. This also means you don't have to contribute the end product or
modified sources back to Open Source, but if you feel like sharing, you are
highly encouraged to do so!

&copy; Copyright 2013-2017 DataArt Apps &copy; All Rights Reserved

Docker Container
=========================================
We have published a DeviceHive docker container so you can utilize docker's virtualization features with DeviceHive. 
Check out [DeviceHive on Docker Hub](https://hub.docker.com/r/devicehive/devicehive/) with the instructions on 
how to use it. You can check dockerfile implementation as well as the script for setting up a new instance running 
under nginx on [DeviceHive Docker](https://github.com/devicehive/devicehive-docker) 

DeviceHive Java installation instructions
=========================================

Prerequisites
-------------
In order to use DeviceHive framework you must have the following components installed and configured:
* [PostgreSQL 9.1](http://www.postgresql.org/download/) or above.
* [Apache Kafka 0.10.0.0](http://kafka.apache.org/downloads.html) or above.
* [Oracle JDK 8](http://www.oracle.com/technetwork/java/javase/downloads/index.html) or [OpenJDK 8](http://openjdk.java.net/)
* [Maven](http://maven.apache.org/download.cgi)
* [DeviceHiveJava source files](https://github.com/devicehive/devicehive-java-server). This is the main part of the [DeviceHive] framework


Build packages
--------------
* Download source code from [GitHub](https://github.com/devicehive/devicehive-java-server) using "Download ZIP" button.
It should always point to recent stable or beta release, but you always can get any other tag or branch.
It also can be done using one of [Git version control client](http://git-scm.com/downloads/guis) or git command line tool.
If you prefer git, clone project using command

`git clone https://github.com/devicehive/devicehive-java-server.git`

After that you can switch to the tag or branch you need. The list of all available releases can be found at
https://github.com/devicehive/devicehive-java-server/releases
Execute following command from ${devicehive-java-server-directory}.

`mvn clean package`

If there are no errors, compilation and packaging are completed and you can go to the next step.

Running Apache Kafka
-----------------------
Start Zookeeper and Apache Kafka brokers as explained at official documentation (`http://kafka.apache.org/documentation.html#quickstart`).
If your Kafka brokers are installed on the different machines, please specify their hostname/ports at app.properties file.
You need to update zookeeper.connect (zookeeper's contact point) and bootstrap.servers (list of brokers) properties.

Starting database
---------------------
* After you have downloaded and installed PostgreSQL (see https://wiki.postgresql.org/wiki/Detailed_installation_guides) 
you have to create new user. This step is required for database migrations to work properly. By default, DH expects that
the username is `postgres` and the password is `12345`. You can change this in the DH configuration files.
* Create database with the name `devicehive` using user that have been created at step 1. This user should be owner of 
database.
* Database schema will be initialized on application startup.

Running application
---------------------
* To start application, you have to start the backend and the frontend. To do this, first run following command:

`java -jar ${devicehive-java-server-directory}/devicehive-backend/target/devicehive-backend-<version>-boot.jar`
 
Wait for the application to start, then run: 

`java -jar ${devicehive-java-server-directory}/devicehive-frontend/target/devicehive-frontend-<version>-boot.jar`

This will start embeded undertow application server on default port 8080 and deploy DeviceHive application.
You can visit http://localhost:8080/dh/swagger from your web browser to start learning the APIs.

You can also set up OAuth2 providers properties. At the moment DeviceHive supports Google Plus, Facebook, Github OAuth2 identity providers.
After registering your application at Google/Facebook/Github you'll be provided with your client id and client secret.
To save them you should use link below:

`http://localhost:8080/DeviceHiveJava/rest/configuration/${name}/set?value=${value}`

For Google:
The parameter “name” should be “google.identity.client.id” and “google.identity.client.secret”.
To allow google authentication flow set “google.identity.allowed” to "true" (by default it's false)

For Facebook:
The parameter “name” should be “facebook.identity.client.id” and “facebook.identity.client.secret”.
To allow facebook authentication flow set “facebook.identity.allowed” to "true" (by default it's false)

For Github:
The parameter “name” should be “github.identity.client.id” and “github.identity.client.secret”.
To allow github authentication flow set “github.identity.allowed” to "true" (by default it's false)
