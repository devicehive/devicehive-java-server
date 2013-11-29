package com.devicehive.client.model.exceptions;

/**
 * This kind of exceptions used when some server error occur: response with internal server error occured or on
 * connection lost event happened
 */
public class HiveServerException extends HiveException {

    private static final String MESSAGE = "Server error";
    private static final long serialVersionUID = 8781352790323264003L;

    public HiveServerException(Integer code) {
        super(MESSAGE, code);
    }

    public HiveServerException(String message, Integer code) {
        super(message, code);
    }
}
