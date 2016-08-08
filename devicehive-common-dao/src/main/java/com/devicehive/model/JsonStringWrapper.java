package com.devicehive.model;

import javax.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class JsonStringWrapper implements HiveEntity {

    private static final long serialVersionUID = -152849186108390497L;
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JsonStringWrapper)) return false;
        JsonStringWrapper that = (JsonStringWrapper) o;
        return Objects.equals(jsonString, that.jsonString);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jsonString);
    }

    @Override
    public String toString() {
        return "JsonStringWrapper{" +
                "jsonString='" + jsonString + '\'' +
                '}';
    }
}
