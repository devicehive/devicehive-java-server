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

DeviceHive Java installation instructions
=========================================

Prerequisites
-------------
In order to use DeviceHive framework you must have the following components installed and configured:
* PostgreSQL 9.1 (9.1 is tested version, but above should work fine too). It can be downloaded from http://www.postgresql.org/download/
* PostgreSQL JDBC driver suitable for your version of PostgreSQL (http://jdbc.postgresql.org/download.html#others)
* Glassfish 4 application server (http://glassfish.java.net/download.html)
* Oracle JDK 7 or OpenJDK 7 (this is requirement for Glassfish 4; Java EE 7 requires JDK 7). Oracle JDK 7 can be
downloaded from http://www.oracle.com/technetwork/java/javase/downloads/index.html. If you would like to use Open JDK
 7 you have to use this link http://openjdk.java.net/install/ or install it from the distribution repository of your
 choice. Package java-1.7.0-openjdk-devel is required for Open JDK
* Maven (http://maven.apache.org/download.cgi) to compile and package db_dhtool and DeviceHiveJava 
* dh_dbtool source files. dh_dbtool.jar will be used to provide necessary database migrations (https://github.com/devicehive/devicehive-java)
* DeviceHiveJava source files. This is the main part of the DeviceHive framework (https://github.com/devicehive/devicehive-java)


Build packages
--------------
* Download source code from https://github.com/devicehive/devicehive-java using "Download ZIP" button. It should always point to recent stable or beta release, but you always can get any other tag or branch. It also can be done using one of Git version control client (http://git-scm.com/downloads/guis) or git command line tool. If you prefer git, clone project using command `git clone https://github.com/devicehive/devicehive-java.git`. After that you can switch to the tag or branch you need. The list of all available releases can be found at https://github.com/devicehive/devicehive-java/releases
* Execute the following command from ${devicehive-java-directory}/tools/dh_dbtools: `mvn clean package`
* Execute the same command from ${devicehive-java-directory}/server.

If this steps are done correctly you will find DeviceHiveJava.war at ${devicehive-java-directory}/server/target and dh_dbtool.jar at ${devicehive-java-directory}/tools/dh_dbtools/target. 
After successful compilation and packaging go to the next step.


Database setup
--------------
* After you have downloaded and installed PostgreSQL (see https://wiki.postgresql.org/wiki/Detailed_installation_guides) you have to create new user. This step is required for database migrations to work properly. 
* Create database using user that have been created at step 1. This user should be owner of database.
* Run dh_dbtool.jar to update your database schema and insert some initial parameters.  Go to dh_dbtool.jar installation directory and run this application using command `java –jar dh_dbtool.jar -migrate -url ${databaseurl} -user ${login} [-password ${password}]`
* The parameter ${databaseurl} is a jdbc connection URL to your database (like jdbc://, user is a database user’s login and password is a user’s password, if required.  To get help use `java –jar dh_dbtool.jar –help`

Glassfish configuration
-----------------------
* Install glassfish 4 as it described in the glassfish installation instructions (see https://glassfish.java.net/docs/4.0/installation-guide.pdf).
* Deploy PostgreSQL jdbc driver to glassfish. Just put postgresql-jdbc4.jar (or another postgresql jdbc driver suitable for your postgresql version) to ${glassfish installation directory}/glassfish/domains/${domain_dir}/lib/ext directory and restart glassfish.
* Then, run server and open ${yourServerName}:4848
* Navigate to Resources -> JDBC -> JDBC Connection Pools. You have to create new JDBC Connection Pool to get access to your database. Configure general settings with following parameters:

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

* Execute server ->  General ->  restart

Deploying application
---------------------
When server is installed and all the required properties are configured, you have to deploy the application. 

* Go to ${yourServerName}:4848
* Open Applications tab.
* Click on Deploy button
* Click on “Select file” button. In the dialog box select DeviceHiveJava.war. Click on “Ok” button
* Launch DeviceHiveJava
* Set up web socket server URL and rest server URL. To do that you have to use link:

http://${yourServerName}:${port}/DeviceHiveJava/rest/configuration/${name}/set?value=${value}

The parameter “name” can be either “websocket.url” or “rest.url” 
The parameter “value” is associated URL for web socket and rest services.
DeviceHive system specifies default login and password. These values can be used to get access for required services.

login: dhadmin

password: dhadmin_#911

Example:
* For rest server URL:

http://localhost:8080/DeviceHiveJava/rest/configuration/rest.url/set?value=http://localhost:8080/devicehive /rest

* For web socket server URL:

http://localhost:8080/DeviceHiveJava/rest/configuration/websocket.url/set?value=ws://localhost:8080/devicehive/websocket

* Use it.

DeviceHive Java update instructions
===================================

* Download source code from https://github.com/devicehive/devicehive-java using "Download ZIP" button. It should
always point to the BRANCH-1.3. It also can be done using one of Git version control client (http://git-scm
.com/downloads/guis) or git command line tool. If you prefer git, clone project using command `git clone
https://github.com/devicehive/devicehive-java.git`. After that you can switch to the tag or branch you need. The list
 of all available releases can be found at https://github.com/devicehive/devicehive-java/releases
* Run dh_dbtool.jar to update your database schema and insert some initial parameters.  Go to dh_dbtool.jar installation directory and run this application using command `java –jar dh_dbtool.jar -migrate -url ${databaseurl} -user ${login} [-password ${password}]`
* The parameter ${databaseurl} is a jdbc connection URL to your database (like jdbc://, user is a database user’s login and password is a user’s password, if required.  To get help use `java –jar dh_dbtool.jar –help`
* Go to ${yourServerName}:4848
* Open Applications tab.
* Click on Undeploy button (is not required)
* Restart glassfish server (is not required)
* Click on Deploy button
* Click on “Select file” button. In the dialog box select DeviceHiveJava.war. Click on “Ok” button
* Launch DeviceHiveJava
Notice, that the all parameters set up for configuration of RESTful and websocket services will be the same. If it is
 required to change these parameters use:

 http://${yourServerName}:${port}/DeviceHiveJava/rest/configuration/${name}/set?value=${value}

 You can get access to this service using any of your administrative accounts.
 For more details see RELEASE_NOTES

DeviceHive Java server settings
===============================
1. rest.url - REST service URL. Required.
2. websocket.url - Websocket service URL. Will be required if websockets used.
3. user.login.maxAttempts - Keeps number of maximum login attempts. Default value: 10.
4. user.login.lastTimeout - Defines when the user should be logged out if there is no user activity. Default value: 1 hour.
5. websocket.ping.timeout - Defines when websocket session is invalid if there is no responses during the ping. Default value: 2 minutes.
 
Admin-Console integration
=========================
To integrate admin-console into devicehive java server follow the next steps:
* Download admin-console from https://github.com/devicehive/devicehive-admin-console 
* Put admin-console to the ${GLASSFISH_HOME_DIR}/domains/${your_domain}/docroot
* Admin-console can be found at http://{yourServerName}:{port}/admin-console