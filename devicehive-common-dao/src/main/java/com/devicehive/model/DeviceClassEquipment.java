package com.devicehive.model;


import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.vo.DeviceClassEquipmentVO;
import com.google.gson.annotations.SerializedName;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

/**
 * TODO JavaDoc
 */
@Entity
@Table(name = "equipment",
       uniqueConstraints = @UniqueConstraint(columnNames = "code"))
@NamedQueries({
                  @NamedQuery(name = "Equipment.getByDeviceClass", query = "select e from Equipment e where e.deviceClass = :deviceClass"),
                  @NamedQuery(name = "Equipment.getByDeviceClassAndId", query =  "select e from Equipment e " +
                                                                                  "join e.deviceClass dc " +
                                                                                  "where e.id = :id and dc.id = :deviceClassId"),
                  @NamedQuery(name = "Equipment.deleteByDeviceClass", query = "delete from Equipment e where e.deviceClass = :deviceClass"),
                  @NamedQuery(name = "Equipment.deleteByIdAndDeviceClass", query = "delete from Equipment e " +
                                                                                   "where e.id = :id " +
                                                                                   "and e.deviceClass in " +
                                                                                   "(select dc from DeviceClass dc where dc.id = :deviceClassId)")
              })
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class DeviceClassEquipment implements HiveEntity {

    private static final long serialVersionUID = -107312669477890926L;
    @SerializedName("id")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonPolicyDef({DEVICECLASS_PUBLISHED, EQUIPMENT_PUBLISHED, EQUIPMENTCLASS_SUBMITTED, DEVICE_PUBLISHED})
    private Long id;
    @SerializedName("name")
    @Column
    @NotNull(message = "name field cannot be null.")
    @Size(min = 1, max = 128, message = "Field cannot be empty. The length of name should not be more than 128 " +
                                        "symbols.")
    @JsonPolicyDef({EQUIPMENT_SUBMITTED, DEVICECLASS_PUBLISHED, EQUIPMENT_PUBLISHED,
                    DEVICE_PUBLISHED, DEVICE_SUBMITTED, DEVICECLASS_SUBMITTED})
    private String name;
    @SerializedName("code")
    @Column
    @NotNull(message = "code field cannot be null.")
    @Size(min = 1, max = 128, message = "Field cannot be empty. The length of code should not be more than 128 " +
                                        "symbols.")
    @JsonPolicyDef({EQUIPMENT_SUBMITTED, DEVICECLASS_PUBLISHED, EQUIPMENT_PUBLISHED,
                    DEVICE_PUBLISHED, DEVICE_SUBMITTED, DEVICECLASS_SUBMITTED})
    private String code;
    @SerializedName("type")
    @Column
    @NotNull(message = "type field cannot be null.")
    @Size(min = 1, max = 128, message = "Field cannot be empty. The length of type should not be more than 128 " +
                                        "symbols.")
    @JsonPolicyDef({EQUIPMENT_SUBMITTED, DEVICECLASS_PUBLISHED, EQUIPMENT_PUBLISHED,
                    DEVICE_PUBLISHED, DEVICE_SUBMITTED, DEVICECLASS_SUBMITTED})
    private String type;
    @SerializedName("data")
    @Embedded
    @AttributeOverrides({
                            @AttributeOverride(name = "jsonString", column = @Column(name = "data"))
                        })
    @JsonPolicyDef({EQUIPMENT_SUBMITTED, DEVICECLASS_PUBLISHED, EQUIPMENT_PUBLISHED,
                    DEVICE_PUBLISHED, DEVICE_SUBMITTED, DEVICECLASS_SUBMITTED})
    private JsonStringWrapper data;
    @ManyToOne
    @JoinColumn(name = "device_class_id", updatable = false)
    @NotNull(message = "device class field cannot be null")
    private DeviceClass deviceClass;
    @Version
    @Column(name = "entity_version")
    private long entityVersion;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getEntityVersion() {
        return entityVersion;
    }

    public void setEntityVersion(long entityVersion) {
        this.entityVersion = entityVersion;
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

    public DeviceClass getDeviceClass() {
        return deviceClass;
    }

    public void setDeviceClass(DeviceClass deviceClass) {
        this.deviceClass = deviceClass;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DeviceClassEquipment equipment = (DeviceClassEquipment) o;

        if (id != null ? !id.equals(equipment.id) : equipment.id != null) return false;
        return !(code != null ? !code.equals(equipment.code) : equipment.code != null);

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (code != null ? code.hashCode() : 0);
        return result;
    }

    public static DeviceClassEquipmentVO convertDeviceClassEquipment(DeviceClassEquipment equipment) {
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

    public static DeviceClassEquipment convertDeviceClassEquipmentVOToEntity(DeviceClassEquipmentVO vo) {
        DeviceClassEquipment en = null;
        if (vo != null) {
            en = new DeviceClassEquipment();
            en.setId(vo.getId());
            en.setData(vo.getData());
            en.setName(vo.getName());
            en.setCode(vo.getCode());
            en.setType(vo.getType());
        }
        return en;
    }



}
