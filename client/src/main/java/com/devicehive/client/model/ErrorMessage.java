package com.devicehive.client.model;


public class ErrorMessage implements HiveEntity{

    private static final long serialVersionUID = -2691319929605273820L;
    private Integer code;
    private String message;

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
