FROM docker.io/bitnami/kafka:3.1

MAINTAINER devicehive

LABEL org.label-schema.url="https://devicehive.com" \
      org.label-schema.vendor="DeviceHive" \
      org.label-schema.vcs-url="https://github.com/devicehive/devicehive-ws-proxy" \
      org.label-schema.name="devicehive-kafka" \
      org.label-schema.version="$DH_VERSION"
      
ENV KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP=CLIENT:PLAINTEXT,EXTERNAL:PLAINTEXT
ENV KAFKA_CFG_LISTENERS=CLIENT://:9092
ENV KAFKA_CFG_ADVERTISED_LISTENERS=CLIENT://kafka:9092
ENV KAFKA_CFG_INTER_BROKER_LISTENER_NAME=CLIENT
ENV KAFKA_CFG_ZOOKEEPER_CONNECT=zookeeper:2181
ENV ALLOW_PLAINTEXT_LISTENER=yes

USER root      
RUN apt update && apt upgrade -y && apt install netcat -y

EXPOSE 9092