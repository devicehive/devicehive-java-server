DeviceHive Dockerized user guide
================================

Quick start
-----------

DeviceHive requires PostgreSQL database in order to operate.
Create directory for PostgreSQL data:

    mkdir -p /data/devicehive

Run a PostgreSQL container:

    docker run -d -v /data/devicehive:/data -e "PGDATA=/data" --name postgresql-devicehive postgres:9.3.5
    
PostgreSQL needs few seconds to become ready. Now you need to give access to PostgreSQL for all users (in the current version of `postgres` container access is enabled only for `postgres` user):

    docker stop postgresql-devicehive
    echo -e "host all all 0.0.0.0/0 trust" >> /data/devicehive/pg_hba.conf
    docker start postgresql-devicehive
    
After that you need to create database `dh` and user `dh_user`:

    echo "CREATE DATABASE dh;CREATE USER dh_user WITH password 'dh_StrOngPasSWorD';GRANT ALL privileges ON DATABASE dh TO dh_user;\q" |
    docker run -ti --link postgresql-devicehive:postgres --rm postgres:9.3.5 sh -c 'exec psql -h "$POSTGRES_PORT_5432_TCP_ADDR" -p "$POSTGRES_PORT_5432_TCP_PORT" -U postgres'
    
After that new database schema should be created with `dh_dbtool.jar`:

    docker run -ti --link postgresql-devicehive:postgres --rm -e "PG_USER=dh_user" -e "PG_PASSWORD=dh_StrOngPasSWorD" -e "PG_DATABASE=dh" "devicehive/devicehive-java:1.3" \
    sh -c 'java -jar /root/dh_dbtool.jar -migrate -url jdbc:postgresql://${POSTGRES_PORT_5432_TCP_ADDR}/${PG_DATABASE} -user ${PG_USER} -password ${PG_PASSWORD}'
    
Now you are finally ready to launch devicehive container. Change `DH_DOMAIN` value to the domain name (or ip address) by which clients from the internet could connect to your server.

    DH_DOMAIN="localhost" # change this to domain/ip accessible for client devices
    docker run -d -p 8080:8080 --name devicehive-server --link postgresql-devicehive:postgres -e "PG_USER=dh_user" -e "PG_PASSWORD=dh_StrOngPasSWorD" -e "PG_DATABASE=dh" -e "DH_DOMAIN=$DH_DOMAIN" -e "DH_PORT=8080" -e "DH_PROTOCOL=http" devicehive/devicehive-java:1.3
    
In about 1 minute DeviceHive admin console will be available at [http://localhost:8080/admin/](http://localhost:8080/admin/)
Admin login/password is `dhadmin` / `dhadmin_#911`. It is stronly adviced to create new admin account and change default password for `dhadmin`

DeviceHive API should be configured on this page: [http://localhost:8080/DeviceHive/](http://localhost:8080/DeviceHive/) (use your domain name or ip instead of `localhost` to setup REST endpoints correctly)
