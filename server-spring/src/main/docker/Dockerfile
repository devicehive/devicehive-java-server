FROM java:8
ADD devicehive-boot.jar devicehive-boot.jar
RUN bash -c 'touch /devicehive-boot.jar'
ENTRYPOINT ["java", "-jar", "/devicehive-boot.jar"]