package com.devicehive.shim.api;

import java.util.Objects;

public abstract class ResponseBody {

    protected String action;

    protected ResponseBody(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResponseBody)) return false;
        ResponseBody that = (ResponseBody) o;
        return Objects.equals(action, that.action);
    }

    @Override
    public int hashCode() {
        return Objects.hash(action);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ResponseBody{");
        sb.append("action='").append(action).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
