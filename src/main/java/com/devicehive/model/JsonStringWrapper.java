package com.devicehive.model;

import javax.persistence.Embeddable;
import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: jkulagina
 * Date: 21.06.13
 * Time: 16:58
 */
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
