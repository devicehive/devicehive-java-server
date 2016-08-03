package com.devicehive.dao.riak.model;

import com.basho.riak.client.api.annotations.RiakIndex;
import com.devicehive.model.Device;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.vo.DeviceEquipmentVO;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class RiakDeviceEquipment {

    private Long id;

    private String code;

    private Date timestamp;

    private JsonStringWrapper parameters;

    private Device device;

    private long entityVersion;

    public long getEntityVersion() {
        return entityVersion;
    }

    public void setEntityVersion(long entityVersion) {
        this.entityVersion = entityVersion;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Date getTimestamp() {
        return ObjectUtils.cloneIfPossible(timestamp);
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = ObjectUtils.cloneIfPossible(timestamp);
    }

    public JsonStringWrapper getParameters() {
        return parameters;
    }

    public void setParameters(JsonStringWrapper parameters) {
        this.parameters = parameters;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    //Riak indexes
    @RiakIndex(name = "device")
    public String getDeviceSi() {
        return device.getGuid();
    }

    public static class Queries {

        public interface Names {

            String DELETE_BY_ID = "DeviceEquipment.deleteById";
            String GET_BY_DEVICE_AND_CODE = "DeviceEquipment.getByDeviceAndCode";
            String DELETE_BY_FK = "DeviceEquipment.deleteByFK";
            String GET_BY_DEVICE = "DeviceEquipment.getByDevice";
        }

        interface Values {

            String DELETE_BY_ID = "delete from DeviceEquipment de where de.id = :id";
            String GET_BY_DEVICE_AND_CODE =
                    "select de from DeviceEquipment de " +
                            "where de.device = :device and de.code = :code";
            String DELETE_BY_FK = "delete from DeviceEquipment de where de.device = :device";
            String GET_BY_DEVICE = "select de from DeviceEquipment de where de.device = :device";
        }

        public static interface Parameters {

            static final String ID = "id";
            static final String DEVICE = "device";
            static final String CODE = "code";
        }

    }

    public static List<DeviceEquipmentVO> convertToVo(List<RiakDeviceEquipment> equipment) {
        if (equipment == null) {
            return Collections.emptyList();
        }
        return equipment.stream().map(RiakDeviceEquipment::convertToVo).collect(Collectors.toList());
    }

    public static List<RiakDeviceEquipment> convertToEntity(List<DeviceEquipmentVO> equipment) {
        if (equipment == null) {
            return Collections.emptyList();
        }
        return equipment.stream().map(RiakDeviceEquipment::convertToEntity).collect(Collectors.toList());
    }

    public static DeviceEquipmentVO convertToVo(RiakDeviceEquipment equipment) {
        DeviceEquipmentVO vo = null;
        if (equipment != null) {
            vo = new DeviceEquipmentVO();
            vo.setCode(equipment.getCode());
            vo.setId(equipment.getId());
            vo.setParameters(equipment.getParameters());
            vo.setTimestamp(equipment.getTimestamp());
        }

        return vo;
    }

    public static RiakDeviceEquipment convertToEntity(DeviceEquipmentVO equipment) {
        RiakDeviceEquipment vo = null;
        if (equipment != null) {
            vo = new RiakDeviceEquipment();
            vo.setCode(equipment.getCode());
            vo.setId(equipment.getId());
            vo.setParameters(equipment.getParameters());
            vo.setTimestamp(equipment.getTimestamp());
        }

        return vo;
    }
}
