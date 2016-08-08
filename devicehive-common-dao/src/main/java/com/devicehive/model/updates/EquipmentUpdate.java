package com.devicehive.model.updates;

import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.HiveEntity;
import com.devicehive.model.JsonStringWrapper;
import com.google.gson.annotations.SerializedName;

import java.util.Optional;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.EQUIPMENT_PUBLISHED;

public class EquipmentUpdate implements HiveEntity {

    private static final long serialVersionUID = -1048095377970919818L;
    @JsonPolicyDef(EQUIPMENT_PUBLISHED)
    Long id;

    @SerializedName("name")
    @JsonPolicyDef(EQUIPMENT_PUBLISHED)
    private Optional<String> name;

    @SerializedName("code")
    @JsonPolicyDef(EQUIPMENT_PUBLISHED)
    private Optional<String> code;

    @SerializedName("type")
    @JsonPolicyDef(EQUIPMENT_PUBLISHED)
    private Optional<String> type;

    @SerializedName("data")
    @JsonPolicyDef(EQUIPMENT_PUBLISHED)
    private Optional<JsonStringWrapper> data;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Optional<String> getName() {
        return name;
    }

    public void setName(Optional<String> name) {
        this.name = name;
    }

    public Optional<String> getCode() {
        return code;
    }

    public void setCode(Optional<String> code) {
        this.code = code;
    }

    public Optional<String> getType() {
        return type;
    }

    public void setType(Optional<String> type) {
        this.type = type;
    }

    public Optional<JsonStringWrapper> getData() {
        return data;
    }

    public void setData(Optional<JsonStringWrapper> data) {
        this.data = data;
    }
}
