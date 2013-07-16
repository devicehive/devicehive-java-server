package com.devicehive.model;

import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public class JsonStringWrapper implements Serializable {

    private String jsonString;

    public JsonStringWrapper() {
    }

    public JsonStringWrapper(String jsonString){
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
        if (!(obj instanceof JsonStringWrapper)){
            return false;
        }
        return obj != null && jsonString.equals(((JsonStringWrapper) obj).getJsonString());
    }

    @Override
    public int hashCode() {
        return jsonString != null ? jsonString.hashCode() : 0;
    }
}
