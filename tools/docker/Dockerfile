FROM ubuntu:14.04
RUN apt-get update
EXPOSE 4848
EXPOSE 8080
EXPOSE 8081

RUN apt-get install -y unzip curl openjdk-7-jdk
RUN curl http://dlc.sun.com.edgesuite.net/glassfish/4.1/release/glassfish-4.1.zip > /root/glassfish-4.1.zip
RUN mkdir -p /opt/ && unzip /root/glassfish-4.1.zip -d /opt && rm /root/glassfish-4.1.zip
RUN curl http://jdbc.postgresql.org/download/postgresql-9.3-1102.jdbc41.jar > /opt/glassfish4/glassfish/domains/domain1/lib/ext/postgresql-9.3-1102.jdbc41.jar
RUN curl https://dl.dropboxusercontent.com/u/7319744/glassfish-4.1/nucleus-grizzly-all.jar > /opt/glassfish4/glassfish/modules/nucleus-grizzly-all.jar
COPY glassfish-init.sh /opt/glassfish4/bin/glassfish-init.sh
RUN chmod a+x /opt/glassfish4/bin/glassfish-init.sh
COPY domain.tpl /opt/glassfish4/glassfish/domains/domain1/config/domain.tpl
COPY DeviceHive-current.war /opt/glassfish4/glassfish/domains/domain1/autodeploy/DeviceHive.war
COPY dh_dbtool-current.jar /root/dh_dbtool.jar
ADD devicehive-admin-console.tar /opt/glassfish4/glassfish/domains/domain1/docroot/admin/

CMD [ "/opt/glassfish4/bin/glassfish-init.sh" ]
