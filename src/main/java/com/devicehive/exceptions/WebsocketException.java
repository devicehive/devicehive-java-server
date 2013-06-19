package com.devicehive.exceptions;

/**
 * Created with IntelliJ IDEA.
 * User: ssidorenko
 * Date: 18.06.13
 * Time: 17:36
 * To change this template use File | Settings | File Templates.
 */
public class WebsocketException extends RuntimeException {

    public WebsocketException(String message, Throwable cause) {
        super(message, cause);
    }

    public WebsocketException(String message) {
        super(message);
    }
}
