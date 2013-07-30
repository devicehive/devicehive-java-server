package com.devicehive.configuration;

public class Constants {

    public static final String PERSISTENCE_UNIT = "devicehive";

    public static final String EMBEDDED_PERSISTENCE_UNIT = "devicehiveEmbedded";

    public static final String COMMAND_TOPIC_DESTINATION_NAME = "CommandTopic";

    public static final String JMS_COMMAND_TOPIC = "java:global/jms/" + COMMAND_TOPIC_DESTINATION_NAME;

    public static final String COMMAND_UPDATE_TOPIC_DESTINATION_NAME = "CommandUpdateTopic";

    public static final String JMS_COMMAND_UPDATE_TOPIC = "java:global/jms/" + COMMAND_UPDATE_TOPIC_DESTINATION_NAME;

    public static final String NOTIFICATION_TOPIC_DESTINATION_NAME = "NotificationTopic";

    public static final String JMS_NOTIFICATION_TOPIC = "java:global/jms/" + NOTIFICATION_TOPIC_DESTINATION_NAME;

    public static final String DATA_SOURCE_CLASS_NAME = "org.apache.derby.jdbc.EmbeddedXADataSource";

    public static final String DATA_SOURCE_NAME = "java:global/jdbc/DeviceHiveEmbeddedDataSource";

    public static final String WEBSOCKET_SERVER_URL = "WebSocketServerUrl";

    public static final String REST_SERVER_URL = "RestServerUrl";

}
