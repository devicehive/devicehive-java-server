package com.devicehive.model.request;

import com.devicehive.model.JsonStringWrapper;
import com.google.gson.annotations.SerializedName;

/**
 * @author Nikolay Loboda
 * @since 19.07.13
 */
public class EquipmentInsert {
    @SerializedName("id")
    private long id;

    @SerializedName("name")
    private String name;

    @SerializedName("code")
    private String code;

    @SerializedName("type")
    private String type;

    @SerializedName("data")
    private JsonStringWrapper data;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public JsonStringWrapper getData() {
        return data;
    }

    public void setData(JsonStringWrapper data) {
        this.data = data;
    }
}
