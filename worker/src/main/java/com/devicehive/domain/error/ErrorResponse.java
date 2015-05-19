package com.devicehive.domain.error;

import org.springframework.http.HttpStatus;

import java.io.Serializable;

/**
 * Created by tmatvienko on 4/19/15.
 */
public class ErrorResponse implements Serializable {
    private int status;
    private String cause;

    public ErrorResponse(HttpStatus status, String cause) {
        this.status = status.value();
        this.cause = cause;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getCause() {
        return cause;
    }

    public void setCause(String cause) {
        this.cause = cause;
    }
}
