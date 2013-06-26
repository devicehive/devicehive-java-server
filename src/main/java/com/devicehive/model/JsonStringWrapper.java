package com.devicehive.model;

import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public class JsonStringWrapper implements Serializable {

    private String jsonString;

    public JsonStringWrapper() {
    }

    public String getJsonString() {
        return jsonString;
    }

    public void setJsonString(String jsonString) {
        this.jsonString = jsonString;
    }
}
