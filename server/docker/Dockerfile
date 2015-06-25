FROM ubuntu:14.04
EXPOSE 22 2181 5432 6379 8080 9001 9092

ENV DEBIAN_FRONTEND noninteractive
RUN /bin/echo debconf shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections
RUN /bin/echo debconf shared/accepted-oracle-license-v1-1 seen true | /usr/bin/debconf-set-selections
RUN apt-get update
RUN apt-get install -y software-properties-common
RUN add-apt-repository ppa:webupd8team/java
RUN apt-get update
RUN apt-get install -y unzip curl oracle-java8-installer openssh-server supervisor postgresql-9.3 psmisc procps redis-server git maven

#postgresql
RUN sed -i 's/^local.*all.*postgres.*peer$/local all postgres trust/' /etc/postgresql/9.3/main/pg_hba.conf
RUN sed -i 's/^local.*all.*all.*peer$/local all all md5/' /etc/postgresql/9.3/main/pg_hba.conf
RUN sed -i "s/#listen_addresses = 'localhost'/listen_addresses = '*'/" /etc/postgresql/9.3/main/postgresql.conf
RUN echo "host all all 0.0.0.0/0 md5" >> /etc/postgresql/9.3/main/pg_hba.conf

#zookeeper+kafka
RUN curl -L -s http://mirror.reverse.net/pub/apache/kafka/0.8.2.0/kafka_2.10-0.8.2.0.tgz > /root/kafka_2.10-0.8.2.0.tgz
RUN tar zxf /root/kafka_2.10-0.8.2.0.tgz -C /opt/ && rm /root/kafka_2.10-0.8.2.0.tgz
RUN mv /opt/kafka_2.10-0.8.2.0 /opt/kafka
RUN ln -s /opt/kafka /opt/zookeeper
COPY zookeeper.properties /opt/zookeeper/config/zookeeper.properties
RUN mkdir /opt/zk-data
COPY server.properties /opt/kafka/config/server.properties
RUN mkdir /opt/kafka-data

#supervisor sshd
COPY supervisord.conf /etc/supervisor/conf.d/supervisord.conf
RUN mkdir -p /var/run/sshd /var/log/supervisor
RUN /bin/bash -c "echo -e \"\n[inet_http_server]\nport=*:9001\nusername=dhadmin\npassword={SHA}$(echo -n 'dhadmin_#911' | sha1sum | awk '{print $1}')\" >> /etc/supervisor/supervisord.conf"

#start script
COPY devicehive-init.sh /devicehive-init.sh
RUN chmod +x /devicehive-init.sh

#user for ssh
RUN useradd -m -s /bin/bash -G sudo dhadmin
RUN usermod --password $(echo "dhadmin_#911" | openssl passwd -1 -stdin) dhadmin

#redis
RUN sed -i "s/daemonize yes/daemonize no/;s/^bind 127.0.0.1/# bind 127.0.0.1/" /etc/redis/redis.conf

#building apps
RUN git clone -b development https://github.com/devicehive/devicehive-java-server.git /home/devicehive-java-server
RUN cd /home/devicehive-java-server && mvn -q clean package -DskipTests -Dserver.log.directory=/var/log
RUN cp /home/devicehive-java-server/server/target/devicehive-boot.jar /root/
RUN rm -rf /home/devicehive-java-server

COPY application-docker.properties /home/application-docker.properties

# Kafka partitions number
ENV KAFKA_PARTITIONS 4

CMD ["/devicehive-init.sh"]
