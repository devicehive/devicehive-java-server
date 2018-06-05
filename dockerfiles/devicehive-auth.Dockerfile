FROM openjdk:8u171-jre-slim

MAINTAINER devicehive

ENV DH_VERSION="3.5.1-SNAPSHOT"

LABEL org.label-schema.url="https://devicehive.com" \
      org.label-schema.vendor="DeviceHive" \
      org.label-schema.vcs-url="https://github.com/devicehive/devicehive-java-server" \
      org.label-schema.name="devicehive-auth" \
      org.label-schema.version="$DH_VERSION"

RUN apt-get update \
    && apt-get install -y netcat-openbsd \
    && rm -rf /var/lib/apt/lists/*

ADD devicehive-auth/target/devicehive-auth-${DH_VERSION}-boot.jar /opt/devicehive/
#start script
ADD dockerfiles/devicehive-auth/devicehive-start.sh /opt/devicehive/
ADD dockerfiles/lib/ /opt/devicehive/lib/

VOLUME ["/var/log/devicehive"]

WORKDIR /opt/devicehive/

ENTRYPOINT ["/bin/sh"]

CMD ["./devicehive-start.sh"]

EXPOSE 8090
