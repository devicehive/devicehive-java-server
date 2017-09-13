FROM openjdk:8u131-jre-alpine

MAINTAINER devicehive

ENV DH_VERSION="3.3.3-SNAPSHOT"

LABEL org.label-schema.url="https://devicehive.com" \
      org.label-schema.vendor="DeviceHive" \
      org.label-schema.vcs-url="https://github.com/devicehive/devicehive-java-server" \
      org.label-schema.name="devicehive-frontend-rdbms" \
      org.label-schema.version="$DH_VERSION"

RUN apk add --no-cache netcat-openbsd

ADD devicehive-frontend/target/devicehive-frontend-${DH_VERSION}-boot.jar /opt/devicehive/
#start script
ADD dockerfiles/devicehive-frontend-rdbms/devicehive-start.sh /opt/devicehive/

VOLUME ["/var/log/devicehive"]

WORKDIR /opt/devicehive/

ENTRYPOINT ["/bin/sh"]

CMD ["./devicehive-start.sh"]

EXPOSE 8080
