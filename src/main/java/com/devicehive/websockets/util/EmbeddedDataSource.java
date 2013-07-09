package com.devicehive.websockets.util;


import com.devicehive.configuration.Constants;

import javax.annotation.sql.DataSourceDefinition;
import javax.ejb.Singleton;

@DataSourceDefinition(
        className = Constants.DATA_SOURCE_CLASS_NAME,
        name = Constants.DATA_SOURCE_NAME,
        portNumber = Constants.PORT_NUMBER,
        serverName = Constants.SERVER_NAME,
        user = "APP",
        password = "APP",
        databaseName = "memory:devicehive;create=true"
)
@Singleton
public class EmbeddedDataSource {
}
