package com.devicehive.model;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: jkulagina
 * Date: 21.06.13
 * Time: 16:58
 */
public class JsonStringWrapper implements Serializable{
    private String str;

    public JsonStringWrapper(String str) {
        this.str = str;
    }

    public String getStr() {
        return str;
    }
}
