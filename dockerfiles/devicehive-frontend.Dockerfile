FROM openjdk:8u151-jre-slim

MAINTAINER devicehive

ENV DH_VERSION="3.4.5-SNAPSHOT"

LABEL org.label-schema.url="https://devicehive.com" \
      org.label-schema.vendor="DeviceHive" \
      org.label-schema.vcs-url="https://github.com/devicehive/devicehive-java-server" \
      org.label-schema.name="devicehive-frontend" \
      org.label-schema.version="$DH_VERSION"

RUN apt-get update \
    && apt-get install -y netcat-openbsd \
    && rm -rf /var/lib/apt/lists/*

ADD devicehive-frontend/target/devicehive-frontend-${DH_VERSION}-boot.jar /opt/devicehive/
#start script
ADD dockerfiles/devicehive-frontend/devicehive-start.sh /opt/devicehive/

VOLUME ["/var/log/devicehive"]

WORKDIR /opt/devicehive/

ENTRYPOINT ["/bin/sh"]

CMD ["./devicehive-start.sh"]

EXPOSE 8080
