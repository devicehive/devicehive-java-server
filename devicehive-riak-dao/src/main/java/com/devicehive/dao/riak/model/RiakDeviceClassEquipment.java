package com.devicehive.dao.riak.model;

import com.devicehive.model.JsonStringWrapper;
import com.devicehive.vo.DeviceClassEquipmentVO;

public class RiakDeviceClassEquipment {

    private Long id;

    private String name;

    private String code;

    private String type;

    private JsonStringWrapper data;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RiakDeviceClassEquipment equipment = (RiakDeviceClassEquipment) o;

        if (id != null ? !id.equals(equipment.id) : equipment.id != null) return false;
        return !(code != null ? !code.equals(equipment.code) : equipment.code != null);

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (code != null ? code.hashCode() : 0);
        return result;
    }

    public static DeviceClassEquipmentVO convertDeviceClassEquipment(RiakDeviceClassEquipment equipment) {
        DeviceClassEquipmentVO vo = null;
        if (equipment != null) {
            vo = new DeviceClassEquipmentVO();
            vo.setCode(equipment.getCode());
            vo.setData(equipment.getData());
            vo.setId(equipment.getId());
            vo.setName(equipment.getName());
            vo.setType(equipment.getType());
        }
        return vo;
    }

    public static RiakDeviceClassEquipment convertDeviceClassEquipmentVOToEntity(DeviceClassEquipmentVO vo) {
        RiakDeviceClassEquipment en = null;
        if (vo != null) {
            en = new RiakDeviceClassEquipment();
            en.setId(vo.getId());
            en.setData(vo.getData());
            en.setName(vo.getName());
            en.setCode(vo.getCode());
            en.setType(vo.getType());
        }
        return en;
    }
}
