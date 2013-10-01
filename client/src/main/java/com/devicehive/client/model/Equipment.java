package com.devicehive.client.model;

import com.devicehive.client.json.strategies.JsonPolicyDef;

import static com.devicehive.client.json.strategies.JsonPolicyDef.Policy.*;

public class Equipment implements HiveEntity {
    private static final long serialVersionUID = -1048095377970919818L;

    @JsonPolicyDef({DEVICECLASS_PUBLISHED, EQUIPMENTCLASS_PUBLISHED, EQUIPMENTCLASS_SUBMITTED})
    private Long id;

    @JsonPolicyDef({EQUIPMENT_SUBMITTED, DEVICECLASS_PUBLISHED, EQUIPMENTCLASS_PUBLISHED, DEVICE_PUBLISHED})
    private String name;

    @JsonPolicyDef({EQUIPMENT_SUBMITTED, DEVICECLASS_PUBLISHED, EQUIPMENTCLASS_PUBLISHED, DEVICE_PUBLISHED})
    private String code;

    @JsonPolicyDef({EQUIPMENT_SUBMITTED, DEVICECLASS_PUBLISHED, EQUIPMENTCLASS_PUBLISHED, DEVICE_PUBLISHED})
    private String type;

    @JsonPolicyDef({EQUIPMENT_SUBMITTED, DEVICECLASS_PUBLISHED, EQUIPMENTCLASS_PUBLISHED, DEVICE_PUBLISHED})
    private JsonStringWrapper data;

    public Equipment() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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
