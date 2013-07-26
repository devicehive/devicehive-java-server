package com.devicehive.messages;

import com.devicehive.model.Message;

/**
 * Types of messages. See {@link Message}. Represents something you can answer with.
 * @author rroschin
 *
 */
public enum MessageType {
    /* DeviceCommand */
    CLIENT_TO_DEVICE_COMMAND,
    /* DeviceCommand which was updated */
    DEVICE_TO_CLIENT_UPDATE_COMMAND,
    /* DeviceNotification */
    DEVICE_TO_CLIENT_NOTIFICATION,
    /* Stateful (websocket) session is closed by device */
    CLOSED_SESSION_DEVICE,
    /* Stateful (websocket) session is closed by client */
    CLOSED_SESSION_CLIENT;
}
