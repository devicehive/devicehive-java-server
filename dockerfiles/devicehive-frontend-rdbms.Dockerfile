FROM openjdk:8-jdk-alpine

MAINTAINER devicehive

ARG SOURCE_REPOSITORY_URL
ARG SOURCE_BRANCH

ENV DH_VERSION="3.3.2-SNAPSHOT"
ENV MAVEN_VERSION="3.3.3"
ENV M2_HOME /usr/share/maven/

RUN apk update \
    && apk add --no-cache netcat-openbsd

# Builds .jar from source code and removes unnecessary dependencies.
RUN apk add --virtual .build-dependencies ca-certificates git openssh openssl curl \
    && mkdir $M2_HOME \
    && cd $M2_HOME \
    && curl -O http://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz \
    && tar -xf apache-maven-$MAVEN_VERSION-bin.tar.gz \
    && mv apache-maven-$MAVEN_VERSION/* $M2_HOME \
    && mkdir -p /opt/devicehive \
    && git clone --depth=1 -b "${SOURCE_BRANCH:-master}" "${SOURCE_REPOSITORY_URL:-https://github.com/devicehive/devicehive-java-server}" /opt/devicehive/source \
    && cd /opt/devicehive/source \
    && $M2_HOME/bin/mvn clean package -Pbooted-rdbms,shim-kafka,!booted-riak -DskipTests \
    && mv /opt/devicehive/source/devicehive-frontend/target/devicehive-frontend-${DH_VERSION}-boot.jar /opt/devicehive/ \
    && rm -rf /opt/devicehive/source \
    && rm -rf $M2_HOME \
    && rm -rf ~/.m2 \
    && apk del .build-dependencies

#start script
ADD devicehive-frontend-rdbms/devicehive-start.sh /opt/devicehive/

VOLUME ["/var/log/devicehive"]

WORKDIR /opt/devicehive/

ENTRYPOINT ["/bin/sh"]

CMD ["./devicehive-start.sh"]

EXPOSE 8080
