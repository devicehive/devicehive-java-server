package com.devicehive.model;

/**
 * Implements response object for error requests
 */
public class ErrorResponse implements HiveEntity {

    public static final String WRONG_SORT_ORDER_PARAM_MESSAGE = "Invalid request parameters, sort order can be 'ASC' or 'DESC' only";
    public static final String INVALID_REQUEST_PARAMETERS_MESSAGE = "Invalid request parameters";
    public static final String JSON_SYNTAX_ERROR_MESSAGE = "JSON syntax error";
    public static final String DEVICE_NOT_FOUND_MESSAGE = "Device not found.";
    public static final String NETWORK_NOT_FOUND_MESSAGE = "Network not found.";
    public static final String FORBIDDEN_MESSAGE = "Access Forbidden";
    private static final long serialVersionUID = 286844689037405279L;


    private Integer error = null;

    private String message = null;

    public Integer getError() {
        return error;
    }

    public ErrorResponse(Integer errorCode, String messageString) {
        error = errorCode;
        message = messageString;
    }

    public ErrorResponse(String messageString) {
        message = messageString;
    }

    public void setError(Integer error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
