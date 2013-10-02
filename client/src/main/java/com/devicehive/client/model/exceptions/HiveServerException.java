package com.devicehive.client.model.exceptions;

public class HiveServerException extends HiveException{

    private static final String MESSAGE = "Server error";

    public HiveServerException() {
        super(MESSAGE);
    }

    public HiveServerException(Integer code) {
        super(MESSAGE, code);
    }
}
