FROM hazelcast/hazelcast:3.8.9

MAINTAINER devicehive

ENV DH_VERSION="3.5.0"

LABEL org.label-schema.url="https://devicehive.com" \
      org.label-schema.vendor="DeviceHive" \
      org.label-schema.vcs-url="https://github.com/devicehive/devicehive-java-server" \
      org.label-schema.name="devicehive-hazelcast" \
      org.label-schema.version="$DH_VERSION"

ADD devicehive-common/target/devicehive-common-${DH_VERSION}-shade.jar /opt/devicehive/
ENV CLASSPATH=/opt/devicehive/devicehive-common-${DH_VERSION}-shade.jar:.
# Custom config is added to the CLASSPATH and must be automaticaly loaded by Hazelcast
ADD dockerfiles/devicehive-hazelcast/hazelcast.xml $HZ_HOME/

EXPOSE 5701
