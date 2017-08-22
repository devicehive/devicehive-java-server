FROM hazelcast/hazelcast:3.8.2

MAINTAINER devicehive

ARG SOURCE_REPOSITORY_URL
ARG SOURCE_BRANCH

ENV DH_VERSION="3.3.2-SNAPSHOT"
ENV MAVEN_VERSION="3.3.3"
ENV M2_HOME /usr/share/maven/

# Base image is using old (and deprecated) java:openjdk-8-jre image. Debian Jessie 'main' and 'update' repos no more contain the same version
# of openjdk-8-jdk-headless package as was used to create this image, so we need to use 'jessie-backports' to upgrade packages first.
RUN apt-get update \
    && apt-get install -y -t jessie-backports git openjdk-8-jdk-headless \
    && mkdir $M2_HOME \
    && cd $M2_HOME \
    && curl -O http://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz \
    && tar -xf apache-maven-$MAVEN_VERSION-bin.tar.gz \
    && mv apache-maven-$MAVEN_VERSION/* $M2_HOME \
    && mkdir -p /opt/devicehive \
    && git clone --depth=1 -b "${SOURCE_BRANCH:-master}" "${SOURCE_REPOSITORY_URL:-https://github.com/devicehive/devicehive-java-server}" /opt/devicehive/source \
    && cd /opt/devicehive/source \
    && $M2_HOME/bin/mvn clean package -Pbooted-rdbms,shim-kafka,!booted-riak -DskipTests \
    && mv /opt/devicehive/source/devicehive-common/target/devicehive-common-${DH_VERSION}-shade.jar /opt/devicehive/ \
    && cd $HZ_HOME \
    && rm -rf /opt/devicehive/source \
    && rm -rf $M2_HOME \
    && rm -rf ~/.m2 \
    && apt-get remove -y --purge git openjdk-8-jdk-headless \
    && apt-get clean

ENV CLASSPATH=/opt/devicehive/devicehive-common-${DH_VERSION}-shade.jar:.
# Config will be in CLASSPATH and must be loaded by hazelcast
ADD devicehive-hazelcast/hazelcast.xml $HZ_HOME/

EXPOSE 5701
