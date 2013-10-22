package com.devicehive.model;


import com.devicehive.json.strategies.JsonPolicyDef;
import com.google.gson.annotations.SerializedName;

import javax.persistence.*;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.HashSet;
import java.util.Set;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

/**
 * TODO JavaDoc
 */
@Entity
@Table(name = "equipment",
        uniqueConstraints = @UniqueConstraint(columnNames = "code"))
@NamedQueries({
        @NamedQuery(name = "Equipment.getByDeviceClass", query = "select e from Equipment e where e.deviceClass = " +
                ":deviceClass"),
        @NamedQuery(name = "Equipment.getByDeviceClassAndId",
                query = "select e from Equipment e join e.deviceClass dc " +
                        "where e.id = :equipmentId and dc.id = :deviceClassId"),
        @NamedQuery(name = "Equipment.deleteByEquipmentList",
                query = "delete from Equipment e where e in :equipmentList"),
        @NamedQuery(name = "Equipment.deleteById", query = "delete from Equipment e where e.id = :id"),
        @NamedQuery(name = "Equipment.deleteByDeviceClass", query = "delete from Equipment e where e.deviceClass = " +
                ":deviceClass"),
        @NamedQuery(name = "Equipment.deleteByIdAndDeviceClass", query = "delete from Equipment e where e.id = :id " +
                "and e.deviceClass in (select dc from DeviceClass dc where dc.id = :deviceClassId)")
})
@Cacheable
public class Equipment implements HiveEntity {

    private static final long serialVersionUID = -107312669477890926L;
    @SerializedName("id")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonPolicyDef({DEVICECLASS_PUBLISHED, EQUIPMENTCLASS_PUBLISHED, EQUIPMENTCLASS_SUBMITTED, DEVICE_PUBLISHED_DEVICE_AUTH, DEVICE_PUBLISHED})
    private Long id;

    @SerializedName("name")
    @Column
    @NotNull(message = "name field cannot be null.")
    @Size(min = 1, max = 128, message = "Field cannot be empty. The length of name should not be more than 128 " +
            "symbols.")
    @JsonPolicyDef({EQUIPMENT_SUBMITTED, DEVICECLASS_PUBLISHED, EQUIPMENTCLASS_PUBLISHED, DEVICE_PUBLISHED_DEVICE_AUTH, DEVICE_PUBLISHED})
    private String name;

    @SerializedName("code")
    @Column
    @NotNull(message = "code field cannot be null.")
    @Size(min = 1, max = 128, message = "Field cannot be empty. The length of code should not be more than 128 " +
            "symbols.")
    @JsonPolicyDef({EQUIPMENT_SUBMITTED, DEVICECLASS_PUBLISHED, EQUIPMENTCLASS_PUBLISHED, DEVICE_PUBLISHED_DEVICE_AUTH, DEVICE_PUBLISHED})
    private String code;

    @SerializedName("type")
    @Column
    @NotNull(message = "type field cannot be null.")
    @Size(min = 1, max = 128, message = "Field cannot be empty. The length of type should not be more than 128 " +
            "symbols.")
    @JsonPolicyDef({EQUIPMENT_SUBMITTED, DEVICECLASS_PUBLISHED, EQUIPMENTCLASS_PUBLISHED, DEVICE_PUBLISHED_DEVICE_AUTH, DEVICE_PUBLISHED})
    private String type;

    @SerializedName("data")
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "jsonString", column = @Column(name = "data"))
    })
    @JsonPolicyDef({EQUIPMENT_SUBMITTED, DEVICECLASS_PUBLISHED, EQUIPMENTCLASS_PUBLISHED, DEVICE_PUBLISHED_DEVICE_AUTH, DEVICE_PUBLISHED})
    private JsonStringWrapper data;

    @ManyToOne
    @JoinColumn(name = "device_class_id", updatable = false)
    @NotNull(message = "device class field cannot be null")
    private DeviceClass deviceClass;

    @Version
    @Column(name = "entity_version")
    private long entityVersion;

    /**
     * Validates equipment representation. Returns set of strings which are represent constraint violations. Set will
     * be empty if no constraint violations found.
     *
     * @param equipment Equipment that should be validated
     * @param validator Validator
     * @return Set of strings which are represent constraint violations
     */
    public static Set<String> validate(Equipment equipment, Validator validator) {
        Set<ConstraintViolation<Equipment>> constraintViolations = validator.validate(equipment);
        Set<String> result = new HashSet<>();
        if (constraintViolations.size() > 0) {
            for (ConstraintViolation<Equipment> cv : constraintViolations)
                result.add(String.format("Error! property: [%s], value: [%s], message: [%s]",
                        cv.getPropertyPath(), cv.getInvalidValue(), cv.getMessage()));
        }
        return result;

    }

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
}
