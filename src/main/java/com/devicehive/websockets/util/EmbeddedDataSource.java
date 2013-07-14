package com.devicehive.websockets.util;


import com.devicehive.configuration.Constants;

import javax.annotation.sql.DataSourceDefinition;
import javax.ejb.Singleton;
import java.sql.Connection;

@DataSourceDefinition(
        className = Constants.DATA_SOURCE_CLASS_NAME,
        name = Constants.DATA_SOURCE_NAME,
        databaseName = "memory:devicehive;create=true",
        transactional = true,
        isolationLevel = Connection.TRANSACTION_SERIALIZABLE,
        initialPoolSize = 2,
        minPoolSize = 2,
        maxPoolSize = 100

)
@Singleton
public class EmbeddedDataSource {
}
