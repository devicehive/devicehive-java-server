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
[MIT license](http://en.wikipedia.org/wiki/MIT_License). This basically means
you can do whatever you want with the software as long as the copyright notice
is included. This also means you don't have to contribute the end product or
modified sources back to Open Source, but if you feel like sharing, you are
highly encouraged to do so!

&copy; Copyright 2013 DataArt Apps &copy; All Rights Reserved

Docker Container
=========================================
We have published DeviceHive docker container so you can utilize docker's virtualization features with DeviceHive. Check out docker [DeviceHive on Docker Hub](https://registry.hub.docker.com/u/devicehive/devicehive-java/) with the instructions on how to use it. You can check dockerfile implemetation as well as the script for setting up a new instance running under nginx on [DeviceHive Java Docker](https://github.com/devicehive/devicehive-java-docker) 

DeviceHive Java installation instructions
=========================================

Prerequisites
-------------
In order to use DeviceHive framework you must have the following components installed and configured:
* [PostgreSQL 9.1](http://www.postgresql.org/download/) or above.
* [PostgreSQL JDBC driver](http://jdbc.postgresql.org/download.html#others) suitable for your version of PostgreSQL
* [Redis 3.0.1](http://redis.io/download) or above.
* [Apache Kafka 2.9.2](http://kafka.apache.org/downloads.html) or above with Zookeeper.
* [Glassfish 4.1](http://glassfish.java.net/download.html) application server (Java EE 7 Full Platform)
* [Oracle JDK 7](http://www.oracle.com/technetwork/java/javase/downloads/index.html) or [OpenJDK 7](http://openjdk.java.net/)
(this is requirement for Glassfish 4.1; Java EE 7 requires JDK 7). JDK 8 will be fine too.
* [Maven](http://maven.apache.org/download.cgi) to compile and package db_dhtool and DeviceHiveJava
* [dh_dbtool source files](https://github.com/devicehive/devicehive-java). dh_dbtool.jar will be used to provide necessary database migrations
* [DeviceHiveJava source files](https://github.com/devicehive/devicehive-java). This is the main part of the [DeviceHive[ framework


Build packages
--------------
* Download source code from [GitHub](https://github.com/devicehive/devicehive-java) using "Download ZIP" button.
It should always point to recent stable or beta release, but you always can get any other tag or branch.
It also can be done using one of [Git version control client](http://git-scm.com/downloads/guis) or git command line tool.
If you prefer git, clone project using command

`git clone https://github.com/devicehive/devicehive-java.git`

After that you can switch to the tag or branch you need. The list of all available releases can be found at
https://github.com/devicehive/devicehive-java/releases
* Execute the following command from ${devicehive-java-directory}/tools/dh_dbtools:

`mvn clean package`

* Execute the same command from ${devicehive-java-directory}/server.

If this steps are done correctly you will find DeviceHiveJava.war at ${devicehive-java-directory}/server/target and dh_dbtool.jar
at ${devicehive-java-directory}/tools/dh_dbtools/target.
After successful compilation and packaging go to the next step.


Database setup
--------------
* After you have downloaded and installed PostgreSQL (see https://wiki.postgresql.org/wiki/Detailed_installation_guides)
you have to create new user. This step is required for database migrations to work properly.
* Create database using user that have been created at step 1. This user should be owner of database.
* Run dh_dbtool.jar to update your database schema and insert some initial parameters.
Go to dh_dbtool.jar installation directory and run this application using command

`java –jar dh_dbtool.jar -migrate -url ${databaseurl} -user ${login} [-password ${password}]`

* The parameter ${databaseurl} is a jdbc connection URL to your database (like jdbc://), ${user} is a database user’s login
and ${password} is a user’s password, if required. To get help use

`java –jar dh_dbtool.jar –help`


Glassfish configuration
-----------------------
* Install Glassfish 4.1 as it described in the [glassfish installation instructions](https://glassfish.java.net/docs/4.0/installation-guide.pdf).
* Deploy PostgreSQL jdbc driver to glassfish. Just put postgresql-jdbc4.jar (or another postgresql jdbc driver suitable
for your postgresql version) to ${glassfish installation directory}/glassfish/domains/${domain_dir}/lib/ext directory and restart glassfish.
* Then, run server and open ${yourServerName}:4848
* Navigate to Resources -> JDBC -> JDBC Connection Pools.
You have to create new JDBC Connection Pool to get access to your database. Configure general settings with following parameters:

Pool Name: Specify some pool name, e.g. DeviceHivePool  
Resource Type: javax.sql.ConnectionPoolDataSource  
Datasource Classname: org.postgresql.ds.PGConnectionPoolDataSource

Specify pool settings at your convenience.

Specify transaction settings as follows:

Transaction Isolation: read-committed
Isolation Level: Guaranteed

Set additional properties:

In the user filed enter your database user login  
In the DatabaseName enter your database name  
In the Password field enter password for access to your database  
In the ServerName field enter your database server name

* Open Resources -> JDBC -> JDBC Resources. Create a new JDBC resource with properties:

JNDI name: jdbc/DeviceHiveDataSource  
Pool name: DeviceHivePool (use recently created pool name)

* Execute server -> General -> restart

Setup managed executor services:

* Open Resources -> Concurrent Resources -> Managed Executor Services. Create concurrent resources with properties:
1. JNDI Name: concurrent/DeviceHiveWaitService  
2. JNDI Name: concurrent/DeviceHiveMessageService

Running Apache Kafka
-----------------------
Start Zookeeper and Apache Kafka brokers as explained at official documentation (`http://kafka.apache.org/documentation.html#quickstart`).
If your Kafka brokers are installed on the different machines, please specify their hostname/ports at app.properties file.
You need to update zookeeper.connect (zookeeper's contactpoint) and metadata.broker.list (list of brokers) properties.

Running Redis
-----------------------
Start Redis as explained at `http://redis.io/download`. Check that Redis configurations (redis.connection.host and
redis.connection.port) in app.properties are correct.

Deploying application
---------------------
When server is installed and all the required properties are configured, you have to deploy the application. 

* Go to ${yourServerName}:4848
* Open Applications tab.
* Click on Deploy button
* Click on “Select file” button. In the dialog box select DeviceHiveJava.war. Click on “Ok” button
* Launch DeviceHiveJava
* Set up web socket server URL and rest server URL. To do that you have to use link:

`http://${yourServerName}:${port}/DeviceHiveJava/rest/configuration/${name}/set?value=${value}`

The parameter “name” can be either “websocket.url” or “rest.url”  
The parameter “value” is associated URL for web socket and rest services.  
DeviceHive system specifies default login and password. These values can be used to get access for required services.

login: dhadmin  
password: dhadmin_#911

Example:
* For rest server URL:

`http://localhost:8080/DeviceHiveJava/rest/configuration/rest.url/set?value=http://localhost:8080/devicehive/rest`

* For web socket server URL:

`http://localhost:8080/DeviceHiveJava/rest/configuration/websocket.url/set?value=ws://localhost:8080/devicehive/websocket`

* Set up OAuth2 providers properties. At the moment DeviceHive supports Google Plus, Facebook, Github OAuth2 identity providers.
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

* Use it.

Docker integration
=========================
1. Build image

Change Dockerfile or defs.sh if it's necessary.
Replace dh.war, cassandra.war and db_tool.jar with new versions and then run build.sh.

2. Publish image

docker push devicehive/devicehive:2.0.0

3. Run container

docker run -d --name=devicehive -p 8080:8080 -p 2222:22 -p 5432:5432 -p 9042:9042 -p 2181:2181 -p 9092:9092 -p 4848:4848 -p 9001:9001 -p 9160:9160 devicehive/devicehive:2.0.0

here are all ports/services proxied but if you don't need some of them just remove unnecessary section "-p port:port"
shortly about ports/services inside the image:
    22 - sshd
    2181 - zookeeper
    4848 - glassfish admin console
    5432 - postgresql
    8080 - devicehive, web access
    9001 - supervisord web admin
    9042 - cassandra CQL native transport
    9092 - kafka broker
    9160 - cassandra Thrift transport

To control container use commands like

docker ps -a

docker stop devicehive (or CONTAINER_ID instead of NAME)
docker start devicehive

docker rm devicehive

Juju integration
=========================
1 Deployment

Devicehive service can be deployed in two ways - charm-by-charm or using bundle.

2. Charm-by-charm deployment

Order of run bash commands:

    juju add-machine --constraints mem=7G
    created machine 4
    juju add-machine --constraints mem=3G
    created machine 5

    let's remember the ids of created machines

    juju deploy cs:~x3v947pl/trusty/postgresql-dh --to 4
    juju deploy cs:~x3v947pl/trusty/zookeeper-dh --to 4
    juju deploy cs:~x3v947pl/trusty/kafka-dh --to 4
    juju deploy cs:~x3v947pl/trusty/redis-dh --to 4
    juju deploy cs:~x3v947pl/trusty/devicehive --to 4
    juju deploy cs:~x3v947pl/trusty/devicehive-worker --to 5
    juju deploy cs:~x3v947pl/trusty/cassandra-dh --to 5

    juju add-relation zookeeper-dh kafka-dh
    juju add-relation devicehive kafka-dh
    juju add-relation devicehive zookeeper-dh
    juju add-relation devicehive redis-dh
    juju add-relation devicehive postgresql-dh
    juju add-relation devicehive-worker zookeeper-dh
    juju add-relation devicehive-worker cassandra-dh

    juju expose devicehive
    juju expose devicehive-worker

Then using command "juju status" get the public addresses of two created machines, eg:

 devicehive:
    charm: cs:~x3v947pl/trusty/devicehive-9
    can-upgrade-to: cs:~x3v947pl/trusty/devicehive-worker-2
    exposed: true
    relations:
      ka:
      - kafka-dh
      pg:
      - postgresql-dh
      re:
      - redis-dh
      zk:
      - zookeeper-dh
    units:
      devicehive/0:
        agent-state: started
        agent-version: 1.20.14
        machine: "4"
        open-ports:
        - 8080/tcp
        public-address: juju-azure-p0v49kbwr1.cloudapp.net
        ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

3. Deploying the bundle

    Step 1 - Install quickstart

    sudo add-apt-repository ppa:juju/stable
    sudo apt-get update
    sudo apt-get install juju-quickstart

    Step 2 - Deploy bundle

    juju-quickstart u/x3v947pl/devicehive-bundle/2

DeviceHive Java update instructions
===================================

* Download [source code](https://github.com/devicehive/devicehive-java) using "Download ZIP" button. It should
always point to the BRANCH-1.3. It also can be done using one of [Git version control client](http://git-scm
.com/downloads/guis) or git command line tool. If you prefer git, clone project using command

`git clone https://github.com/devicehive/devicehive-java.git`

After that you can switch to the tag or branch you need. The list of all available releases can be found at https://github.com/devicehive/devicehive-java/releases
* Run dh_dbtool.jar to update your database schema and insert some initial parameters.
Go to dh_dbtool.jar installation directory and run this application using command

`java –jar dh_dbtool.jar -migrate -url ${databaseurl} -user ${login} [-password ${password}]`

* The parameter ${databaseurl} is a jdbc connection URL to your database
(like jdbc://), ${user} is a database user’s login and ${password} is a user’s password, if required. To get help use

`java –jar dh_dbtool.jar –help`

* Go to ${yourServerName}:4848
* Open Applications tab.
* Click on Undeploy button (is not required)
* Restart glassfish server (is not required)
* Click on Deploy button
* Click on “Select file” button. In the dialog box select DeviceHiveJava.war. Click on “Ok” button
* Launch DeviceHiveJava

**Glassfish 4.1 is required!**

Notice, that the all parameters set up for configuration of RESTful and websocket services will be the same. If it is
 required to change these parameters use:

`http://${yourServerName}:${port}/DeviceHiveJava/rest/configuration/${name}/set?value=${value}`

You can get access to this service using any of your administrative accounts.  
For more details see [release notes](https://github.com/devicehive/devicehive-java/blob/master/RELEASE_NOTES.md)

DeviceHive Java server settings
===============================
1. `rest.url` - REST service URL. Required.
2. `websocket.url` - Websocket service URL. Will be required if websockets used.
3. `user.login.maxAttempts` - Keeps number of maximum login attempts. Default value: 10.
4. `user.login.lastTimeout` - Defines when the user should be logged out if there is no user activity. Default value: 1 hour.
5. `websocket.ping.timeout` - Defines when websocket session is invalid if there is no responses during the ping. Default value: 2 minutes.
 
Admin-Console integration
=========================
To integrate admin-console into devicehive java server follow the next steps:
* Download [admin-console](https://github.com/devicehive/devicehive-admin-console)
* Put admin-console to the `${GLASSFISH_HOME_DIR}/domains/${your_domain}/docroot`
* Admin-console can be found at `http://{yourServerName}:{port}/admin-console`