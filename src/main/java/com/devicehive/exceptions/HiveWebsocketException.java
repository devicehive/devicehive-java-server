package com.devicehive.exceptions;

/**
 * Created with IntelliJ IDEA.
 * User: ssidorenko
 * Date: 18.06.13
 * Time: 17:36
 * To change this template use File | Settings | File Templates.
 */
public class HiveWebsocketException extends RuntimeException {

    public HiveWebsocketException(String message, Throwable cause) {
        super(message, cause);
    }

    public HiveWebsocketException(String message) {
        super(message);
    }
}
