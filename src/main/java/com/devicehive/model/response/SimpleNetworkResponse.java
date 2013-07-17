package com.devicehive.model.response;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * @author Nikolay Loboda
 * @since 17.07.13
 */
public class SimpleNetworkResponse implements Serializable {

    @SerializedName("id")
    private long id;

    @SerializedName("key")
    private String key;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
