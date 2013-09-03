package com.devicehive.model.view;

import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.domain.Equipment;
import com.devicehive.model.HiveEntity;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.model.NullableWrapper;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

public class EquipmentView implements HiveEntity {
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

    public EquipmentView() {
    }

    public EquipmentView(Equipment equipment) {
        convertFrom(equipment);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public NullableWrapper<String> getName() {
        return name;
    }

    public void setName(NullableWrapper<String> name) {
        this.name = name;
    }

    public NullableWrapper<String> getCode() {
        return code;
    }

    public void setCode(NullableWrapper<String> code) {
        this.code = code;
    }

    public NullableWrapper<String> getType() {
        return type;
    }

    public void setType(NullableWrapper<String> type) {
        this.type = type;
    }

    public NullableWrapper<JsonStringWrapper> getData() {
        return data;
    }

    public void setData(NullableWrapper<JsonStringWrapper> data) {
        this.data = data;
    }


    public Equipment convertTo() {
        Equipment equipment = new Equipment();
        equipment.setId(id);
        if (data != null) {
            equipment.setData(data.getValue());
        }
        if (code != null) {
            equipment.setCode(code.getValue());
        }
        if (type != null) {
            equipment.setType(type.getValue());
        }
        if (name != null) {
            equipment.setName(name.getValue());
        }
        return equipment;
    }

    public void convertFrom(Equipment equipment){
        if (equipment == null){
            return;
        }
        id = equipment.getId();
        data = new NullableWrapper<>(equipment.getData());
        code = new NullableWrapper<>(equipment.getCode());
        type = new NullableWrapper<>(equipment.getType());
        name = new NullableWrapper<>(equipment.getName());
    }
}
