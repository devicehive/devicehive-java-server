[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=flat-square)](LICENSE)

DeviceHive Java server
======================

[DeviceHive]: http://devicehive.com "DeviceHive framework"
[DataArt]: http://dataart.com "DataArt"

DeviceHive turns any connected device into the part of Internet of Things.
It provides the communication layer, control software and multi-platform
libraries to bootstrap development of smart energy, home automation, remote
sensing, telemetry, remote control and monitoring software and much more.

Connect embedded Linux using Python, Node.js or Java libraries and JSON format.
Write and read your data via REST, Websockets or MQTT, explore visualization on [Grafana](https://grafana.com/plugins/devicehive-devicehive-datasource/installation) charts.

Develop client applications using HTML5/JavaScript and Android libraries.
Leave communications to DeviceHive and focus on actual product and innovation.

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
DeviceHive could be deployed manually, via Docker Compose or to Kubernetes cluster.
Our suggestion is to start from [Docker Compose](https://docs.docker.com/compose/) - the easiest way to start your 
mastering DeviceHive capabilities. Instructions could be found [here](https://github.com/devicehive/devicehive-docker/tree/master/rdbms-image).
In case you're more familiar with [Kubernetes](https://kubernetes.io/), please follow this 
[link](https://github.com/devicehive/devicehive-docker/tree/master/k8s) for detailed instructions. 

DeviceHive Java installation instructions
=========================================

Though docker-compose installation is the most developer-friendly way of running DeviceHive locally, sometimes it's required
to build and start project manually. Below you can find detailed instructions on that.

Prerequisites
-------------
In order to use DeviceHive framework you must have the following components installed and configured:
* [PostgreSQL 9.1](http://www.postgresql.org/download/) or above.
* [Apache Kafka 0.10.0.0](http://kafka.apache.org/downloads.html) or above.
* [DeviceHive Websocket Proxy](https://github.com/devicehive/devicehive-ws-proxy) running (relies on Kafka, 
so should be started only when Kafka is up and running).
* [Hazelcast IMDG](https://hazelcast.com/use-cases/imdg/).
* [Oracle JDK 8](http://www.oracle.com/technetwork/java/javase/downloads/index.html) or [OpenJDK 8](http://openjdk.java.net/).
* [Maven](http://maven.apache.org/download.cgi).
* [DeviceHiveJava source files](https://github.com/devicehive/devicehive-java-server). This is the main part of the [DeviceHive] framework.


Build packages
--------------
Download source code from [GitHub](https://github.com/devicehive/devicehive-java-server) using "Download ZIP" button.
It should always point to recent stable or beta release, but you always can get any other tag or branch.
It also can be done using one of [Git version control client](http://git-scm.com/downloads/guis) or git command line tool.
If you prefer git, clone project using command

`git clone https://github.com/devicehive/devicehive-java-server.git`

After that you can switch to the tag or branch you need. The list of all available releases can be found at
https://github.com/devicehive/devicehive-java-server/releases.
Execute following command from ${devicehive-java-server-directory}.

`mvn clean package`

If there are no errors, compilation and packaging are completed and you can go to the next step.

Running Apache Kafka
-----------------------
Start Zookeeper and Apache Kafka brokers as explained at official documentation (`http://kafka.apache.org/documentation.html#quickstart`).
If your Kafka brokers are installed on the different machines, please specify their hostname/ports at app.properties file.
You need to update zookeeper.connect (zookeeper's contact point) and bootstrap.servers (list of brokers) properties.

Running Hazelcast
-----------------------
To start, download Hazelcast IMDG 3.8.1 from official site (`https://hazelcast.org/download/`), extract to local drive and create in Hazelcast bin folder file hzstart.sh with following contents:

```bash
export JAVA_OPTS="$JAVA_OPTS -cp /path/to/jar/from/devicehive-hazelcast/devicehive-common-<version>-shade.jar:/path/to/HAZELCAST_HOME/lib/hazelcast-all-3.8.1.jar"
./start.sh

```

Replace

```xml
<serialization>
   <portable-version>0</portable-version>
</serialization>
```

with

```xml
<serialization>
   <portable-version>0</portable-version>
   <portable-factories>
        <portable-factory factory-id="1">com.devicehive.model.DevicePortableFactory</portable-factory>
   </portable-factories>
</serialization>
```

in hazelcast.xml localted in bin folder of hazelcast. Also replace all the map and and multimap sections of hazelcast.xml with:

```
<map name="default">
  <eviction-policy>LRU</eviction-policy>
</map>
<map name="NOTIFICATIONS-MAP">
  <time-to-live-seconds>120</time-to-live-seconds>
</map>
<map name="COMMANDS-MAP">
  <time-to-live-seconds>120</time-to-live-seconds>
</map>
<multimap name="default">
  <backup-count>0</backup-count>
  <async-backup-count>1</async-backup-count>
  <value-collection-type>SET</value-collection-type>
</multimap>
```


Run hzstart.sh.

Starting database
---------------------
* After you have downloaded and installed PostgreSQL (see https://wiki.postgresql.org/wiki/Detailed_installation_guides) 
you have to create new user. This step is required for database migrations to work properly. By default, DH expects that
the username is `postgres` and the password is `12345`. You can change this in the DH configuration files.
* Create database with the name `devicehive` using user that have been created at step 1. This user should be owner of 
database.
* Database schema will be initialized on application startup.

Checking properties
---------------------

Each microservice has its own `src/main/resources/application.properties` file which contains all application-level 
configurations (db credentials, hazelcast address, kafka props etc.). Please check them before building application in 
order to avoid problems at runtime.

You can also override these values by passing them to JVM while running `java -Dapplication.property.name=application.property.name -jar`.
For example: 
```
java -Dhazelcast.cluster.members=0.0.0.1:5701 -jar ${devicehive-jar}.jar
java -Dbootstrap.servers=0.0.0.1:9092 -jar ${devicehive-jar}.jar
java -Dproxy.connect=0.0.0.1:3000 -jar ${devicehive-jar}.jar
```

DB connection properties are managed inside `devicehive-rdbms-dao/src/main/resources/application-persistence.properties`.
To override them do the same:
```
java -Dspring.datasource.url=jdbc:postgresql://0.0.0.1:5432/devicehive -jar ${devicehive-jar}.jar
java -Dspring.datasource.username=test -Dspring.datasource.password=test -jar ${devicehive-jar}.jar
```

Running application
---------------------
DeviceHive ecosystem contains of 3 mandatory and 1 optional services, namely Backend, Frontend, Auth and Plugin 
management (optional) micro services.

* To start application, first run following command:

`java -jar ${devicehive-java-server-directory}/devicehive-backend/target/devicehive-backend-<version>-boot.jar`
 
This will start Backend. Wait for the application to start, then run: 

`java -jar ${devicehive-java-server-directory}/devicehive-frontend/target/devicehive-frontend-<version>-boot.jar`

and 

`java -jar ${devicehive-java-server-directory}/devicehive-auth/target/devicehive-auth-<version>-boot.jar`

This will start embedded undertow application server on default port 8080 and deploy DeviceHive application.
You can visit `http://localhost:8080/dh/swagger` from your web browser to start learning the frontend's APIs.
Also you can visit `http://localhost:8090/dh/swagger` from your web browser to start learning the auth's APIs.

For devicehive-frontend and devicehive-backend logging level can be changed by adding the following properties to the command above:

`-Droot.log.level=value1 -Dcom.devicehive.log.level=value2`

The values can be: TRACE, DEBUG, INFO, WARN, ERROR. If the properties are absent the default values will be used.
For devicehive-frontend and devicehive-auth default values for value1 and value2 are WARN and INFO correspondingly.
For devicehive-backend the default value for both is INFO.

Plugin management service
---------------------

There's one optional service inside DeviceHive ecosystem - Plugin Management service. It allows to register and to update 
DeviceHive plugins (that allow customers to implement their own business logic without diving into DeviceHive source code)
via RESTful API.

To start it simply run following command:

`java -jar ${devicehive-java-server-directory}/devicehive-plugin/target/devicehive-plugin-<version>-boot.jar`

Service will be started on 8110 port by default, so you can visit its swagger at `http://localhost:8110/dh/swagger`
