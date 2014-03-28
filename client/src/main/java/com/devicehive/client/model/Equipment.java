package com.devicehive.client.model;

import com.devicehive.client.impl.json.strategies.JsonPolicyDef;

import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.*;

/**
 * Represents an equipment which is installed on devices.
 * For more details see <a href="http://www.devicehive.com/restful#Reference/Equipment"></a>
 */
public class Equipment implements HiveEntity {
    private static final long serialVersionUID = -1048095377970919818L;

    @JsonPolicyDef({DEVICECLASS_PUBLISHED, EQUIPMENTCLASS_PUBLISHED, EQUIPMENTCLASS_SUBMITTED})
    private Long id;

    @JsonPolicyDef({EQUIPMENT_SUBMITTED, DEVICECLASS_PUBLISHED, EQUIPMENTCLASS_PUBLISHED, DEVICE_PUBLISHED})
    private NullableWrapper<String> name;

    @JsonPolicyDef({EQUIPMENT_SUBMITTED, DEVICECLASS_PUBLISHED, EQUIPMENTCLASS_PUBLISHED, DEVICE_PUBLISHED})
    private NullableWrapper<String> code;

    @JsonPolicyDef({EQUIPMENT_SUBMITTED, DEVICECLASS_PUBLISHED, EQUIPMENTCLASS_PUBLISHED, DEVICE_PUBLISHED})
    private NullableWrapper<String> type;

    @JsonPolicyDef({EQUIPMENT_SUBMITTED, DEVICECLASS_PUBLISHED, EQUIPMENTCLASS_PUBLISHED, DEVICE_PUBLISHED})
    private NullableWrapper<JsonStringWrapper> data;

    public Equipment() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return NullableWrapper.value(name);
    }

    public void setName(String name) {
        this.name = NullableWrapper.create(name);
    }

    public void removeName() {
        this.name = null;
    }

    public String getCode() {
        return NullableWrapper.value(code);
    }

    public void setCode(String code) {
        this.code = NullableWrapper.create(code);
    }

    public void removeCode() {
        this.code = null;
    }


    public String getType() {
        return NullableWrapper.value(type);
    }

    public void setType(String type) {
        this.type = NullableWrapper.create(type);
    }

    public void removeType() {
        this.type = null;
    }

    public JsonStringWrapper getData() {
        return NullableWrapper.value(data);
    }

    public void setData(JsonStringWrapper data) {
        this.data = NullableWrapper.create(data);
    }

    public void removeData() {
        this.data = null;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Equipment{");
        sb.append("id=").append(id);
        sb.append(", name=").append(name);
        sb.append(", code=").append(code);
        sb.append(", type=").append(type);
        sb.append(", data=").append(data);
        sb.append('}');
        return sb.toString();
    }
}
