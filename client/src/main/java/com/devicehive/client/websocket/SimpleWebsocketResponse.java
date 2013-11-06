package com.devicehive.client.websocket;


import com.devicehive.client.model.HiveEntity;
import com.google.gson.annotations.SerializedName;

public class SimpleWebsocketResponse implements HiveEntity{

    private static final long serialVersionUID = 5718896503617891330L;
    @SerializedName("action")
    private String action;
    @SerializedName("requestId")
    private String requestId;
    @SerializedName("status")
    private String status;
    @SerializedName("code")
    private Integer code;
    @SerializedName("error")
    private String error;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
