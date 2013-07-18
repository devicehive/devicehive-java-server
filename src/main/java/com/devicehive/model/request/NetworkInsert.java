package com.devicehive.model.request;

import com.google.gson.annotations.SerializedName;

/**
 * This class is used to parse user's requests
 *
 * @author Nikolay Loboda
 * @since 18.07.13
 */
public class NetworkInsert {

    @SerializedName("key")
    private String key;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

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
