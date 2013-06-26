package com.devicehive.configuration;

/**
 * Created with IntelliJ IDEA.
 * User: ssidorenko
 * Date: 21.06.13
 * Time: 12:45
 * To change this template use File | Settings | File Templates.
 */
public class Constants {

    public static final String PERSISTENCE_UNIT = "devicehive";

    public static final String DATASOURCE = "jdbc/DeviceHivePool";

    public static final String EMBEDDED_PERSISTENCE_UNIT = "devicehiveEmbedded";

    public static final String EMBEDDED_DATASOURCE = "jdbc/DeviceHiveEmbeddedDataSource";


    public static final String JMS_TOPIC_FACTORY = "jms/TopicFactory";

    public static final String JMS_COMMAND_TOPIC = "jms/CommandTopic";

    public static final String JMS_COMMAND_UPDATE_TOPIC = "jms/CommandUpdateTopic";

    public static final String JMS_NOTIFICATION_TOPIC = "jms/NotificationTopic";
}
