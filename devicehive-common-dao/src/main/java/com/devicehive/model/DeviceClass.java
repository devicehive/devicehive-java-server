package com.devicehive.model;


import com.basho.riak.client.api.annotations.RiakIndex;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.vo.DeviceClassEquipmentVO;
import com.devicehive.vo.DeviceClassVO;
import com.devicehive.vo.DeviceClassWithEquipmentVO;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

/**
 * Represents a device class which holds meta-information about devices.
 */
@Entity
@Table(name = "device_class")
@NamedQueries({
        @NamedQuery(name = "DeviceClass.findByName", query = "select d from DeviceClass d where d.name = :name")
})
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class DeviceClass implements HiveEntity {

    public static final String NAME_COLUMN = "name";
    private static final long serialVersionUID = 8091624406245592117L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonPolicyDef(
            {DEVICE_PUBLISHED, NETWORK_PUBLISHED, DEVICECLASS_LISTED,
                    DEVICECLASS_PUBLISHED, DEVICECLASS_SUBMITTED})
    private Long id;

    @Column
    @NotNull(message = "name field cannot be null.")
    @Size(min = 1, max = 128, message = "Field cannot be empty. The length of name should not be more than 128 " +
            "symbols.")
    @JsonPolicyDef(
            {DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED, DEVICECLASS_LISTED,
                    DEVICECLASS_PUBLISHED})
    private String name;

    @Column(name = "is_permanent")
    @JsonPolicyDef(
        {DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED, DEVICECLASS_LISTED,
         DEVICECLASS_PUBLISHED})
    private Boolean isPermanent;

    @Column(name = "offline_timeout")
    @JsonPolicyDef(
        {DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED, DEVICECLASS_LISTED,
         DEVICECLASS_PUBLISHED})
    private Integer offlineTimeout;

    @Embedded
    @AttributeOverrides({
                            @AttributeOverride(name = "jsonString", column = @Column(name = "data"))
                        })
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED, DEVICECLASS_LISTED, DEVICECLASS_PUBLISHED})
    private JsonStringWrapper data;

    @OneToMany(mappedBy = "deviceClass", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JsonPolicyDef({DEVICECLASS_PUBLISHED, DEVICE_PUBLISHED})
    private Set<DeviceClassEquipment> equipment;

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

    @RiakIndex(name = "name")
    public void setNameRi(String nameRi) {
        this.name = nameRi;
    }

    @RiakIndex(name = "name")
    public String getNameRi() {
        return name;
    }

    public Boolean getPermanent() {
        return isPermanent;
    }

    public void setPermanent(Boolean permanent) {
        isPermanent = permanent;
    }

    public Integer getOfflineTimeout() {
        return offlineTimeout;
    }

    public void setOfflineTimeout(Integer offlineTimeout) {
        this.offlineTimeout = offlineTimeout;
    }

    public JsonStringWrapper getData() {
        return data;
    }

    public void setData(JsonStringWrapper data) {
        this.data = data;
    }

    public Set<DeviceClassEquipment> getEquipment() {
        return equipment;
    }

    public void setEquipment(Set<DeviceClassEquipment> equipment) {
        this.equipment = equipment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DeviceClass that = (DeviceClass) o;

        return !(id != null ? !id.equals(that.id) : that.id != null);

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    public static DeviceClassWithEquipmentVO convertToVo(DeviceClass deviceClass) {
        DeviceClassWithEquipmentVO vo = null;
        if (deviceClass != null) {
            vo = new DeviceClassWithEquipmentVO();
            vo.setName(deviceClass.getName());
            vo.setData(deviceClass.getData());
            vo.setId(deviceClass.getId());
            vo.setIsPermanent(deviceClass.getPermanent());
            vo.setOfflineTimeout(deviceClass.getOfflineTimeout());

            if (deviceClass.getEquipment() != null) {
                Stream<DeviceClassEquipmentVO> eqVos = deviceClass.getEquipment().stream().map(DeviceClassEquipment::convertDeviceClassEquipment);
                vo.setEquipment(eqVos.collect(Collectors.toSet()));
            }
        }
        return vo;
    }

    public static DeviceClass convertToEntity(DeviceClassVO vo) {
        DeviceClass en = null;
        if (vo != null) {
            en = new DeviceClass();
            en.setId(vo.getId());
            en.setData(vo.getData());
            en.setName(vo.getName());
            en.setOfflineTimeout(vo.getOfflineTimeout());
            en.setPermanent(vo.getIsPermanent());
        }
        return en;
    }

    public static DeviceClass convertWithEquipmentToEntity(DeviceClassWithEquipmentVO vo) {
        DeviceClass en = convertToEntity(vo);
        if (en != null) {
            if (vo.getEquipment() != null) {
                Set<DeviceClassEquipment> equipmentSet = vo.getEquipment().stream().map(DeviceClassEquipment::convertDeviceClassEquipmentVOToEntity).collect(Collectors.toSet());
                for (DeviceClassEquipment equipment : equipmentSet) {
                    equipment.setDeviceClass(en);
                }
                en.setEquipment(equipmentSet);
            } else {
                en.setEquipment(Collections.emptySet());
            }
        }
        return en;
    }

}
