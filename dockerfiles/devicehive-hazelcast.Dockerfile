FROM hazelcast/hazelcast:5.1.2

USER root
RUN apk update \
    && apk upgrade \
    && apk add --no-cache openjdk17

USER hazelcast

MAINTAINER devicehive

ENV DH_VERSION="4.0.0"

LABEL org.label-schema.url="https://devicehive.com" \
      org.label-schema.vendor="DeviceHive" \
      org.label-schema.vcs-url="https://github.com/devicehive/devicehive-java-server" \
      org.label-schema.name="devicehive-hazelcast" \
      org.label-schema.version="$DH_VERSION"

ADD devicehive-common/target/devicehive-common-${DH_VERSION}-shade.jar /opt/devicehive/
ENV CLASSPATH=/opt/devicehive/devicehive-common-${DH_VERSION}-shade.jar:.

ADD dockerfiles/devicehive-hazelcast/hazelcast.xml ${HZ_HOME}
ENV JAVA_OPTS -Dhazelcast.config=${HZ_HOME}/hazelcast.xml

EXPOSE 5701
