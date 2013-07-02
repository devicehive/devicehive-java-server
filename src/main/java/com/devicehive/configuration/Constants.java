package com.devicehive.configuration;

public class Constants {

    public static final String PERSISTENCE_UNIT = "devicehive";

    public static final String DATASOURCE = "jdbc/DeviceHivePool";

    public static final String EMBEDDED_PERSISTENCE_UNIT = "devicehiveEmbedded";

    public static final String EMBEDDED_DATASOURCE = "jdbc/DeviceHiveEmbeddedDataSource";


    public static final String JMS_TOPIC_FACTORY = "jms/TopicFactory";

    public static final String JMS_COMMAND_TOPIC = "jms/CommandTopic";

    public static final String JMS_COMMAND_UPDATE_TOPIC = "jms/CommandUpdateTopic";

    public static final String JMS_NOTIFICATION_TOPIC = "jms/NotificationTopic";

    public static final String WEBSOCKET_SERVER_URL = "WebSocketServerUrl";

    public static final String REST_SERVER_URL = "RestServerUrl";
}
