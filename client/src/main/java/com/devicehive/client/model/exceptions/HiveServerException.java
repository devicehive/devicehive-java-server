package com.devicehive.client.model.exceptions;

/**
 * TODO
 */
public class HiveServerException extends HiveException {

    private static final String MESSAGE = "Server error";
    private static final long serialVersionUID = 8781352790323264003L;

    public HiveServerException() {
        super(MESSAGE);
    }

    public HiveServerException(Integer code) {
        super(MESSAGE, code);
    }

    public HiveServerException(String message, Integer code) {
        super(message, code);
    }
}
