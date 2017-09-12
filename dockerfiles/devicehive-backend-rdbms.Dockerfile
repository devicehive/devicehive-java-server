FROM openjdk:8u131-jre-alpine

MAINTAINER devicehive

ENV DH_VERSION="3.3.2"

LABEL org.label-schema.url="https://devicehive.com" \
      org.label-schema.vendor="DeviceHive" \
      org.label-schema.vcs-url="https://github.com/devicehive/devicehive-java-server" \
      org.label-schema.name="devicehive-backend-rdbms" \
      org.label-schema.version="$DH_VERSION"

RUN apk add --no-cache netcat-openbsd

ADD devicehive-backend/target/devicehive-backend-${DH_VERSION}-boot.jar /opt/devicehive/
#start script
ADD dockerfiles/devicehive-backend-rdbms/devicehive-start.sh /opt/devicehive/

VOLUME ["/var/log/devicehive"]

WORKDIR /opt/devicehive/

ENTRYPOINT ["/bin/sh"]

CMD ["./devicehive-start.sh"]
