package com.devicehive.domain.wrappers;

import java.io.Serializable;

/**
 * Created by tmatvienko on 2/24/15.
 */
public class JsonStringWrapper implements Serializable {

    private String jsonString;

    public JsonStringWrapper() {
    }

    public JsonStringWrapper(String jsonString) {
        this.jsonString = jsonString;
    }

    public String getJsonString() {
        return jsonString;
    }

    public void setJsonString(String jsonString) {
        this.jsonString = jsonString;
    }

    @Override
    public boolean equals(Object obj) {

        if (obj == null || !(obj instanceof JsonStringWrapper)) {
            return false;
        }

        return jsonString.equals(((JsonStringWrapper) obj).getJsonString());
    }

    @Override
    public int hashCode() {
        return jsonString != null ? jsonString.hashCode() : 0;
    }
}
