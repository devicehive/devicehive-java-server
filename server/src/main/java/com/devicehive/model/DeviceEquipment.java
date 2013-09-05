package com.devicehive.model;


import com.devicehive.json.strategies.JsonPolicyDef;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.ObjectUtils;

import javax.persistence.*;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.DEVICE_EQUIPMENT_SUBMITTED;


@Entity
@Table(name = "device_equipment")
@NamedQueries({
        @NamedQuery(name = "DeviceEquipment.getByCode", query = "select de from  DeviceEquipment de where de.code = " +
                ":code"),
        @NamedQuery(name = "DeviceEquipment.deleteById", query = "delete from DeviceEquipment de where de.id = :id"),
        @NamedQuery(name = "DeviceEquipment.getByDeviceAndCode",
                query = "select de from DeviceEquipment de where de.device = :device and de.code = :code"),
        @NamedQuery(name = "DeviceEquipment.deleteByFK", query = "delete from DeviceEquipment de where de.device = " +
                ":device"),
        @NamedQuery(name = "DeviceEquipment.getByDevice",
                query = "select de from DeviceEquipment de where de.device = " +
                        ":device")
})
@Cacheable
public class DeviceEquipment implements HiveEntity {

    private static final long serialVersionUID = 479737367629574073L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @SerializedName("sid")//TODO: dirty hack, remove it
    private Long id;

    @Column
    @NotNull(message = "code field cannot be null.")
    @Size(min = 1, max = 128, message = "Field cannot be empty. The length of code should not be more than 128 " +
            "symbols.")
    @SerializedName("id")
    @JsonPolicyDef(DEVICE_EQUIPMENT_SUBMITTED)
    private String code;

    @Column
    @NotNull
    @JsonPolicyDef(DEVICE_EQUIPMENT_SUBMITTED)
    private Timestamp timestamp;

    @SerializedName("parameters")
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "jsonString", column = @Column(name = "parameters"))
    })
    @JsonPolicyDef(DEVICE_EQUIPMENT_SUBMITTED)
    private JsonStringWrapper parameters;

    @ManyToOne
    @JoinColumn(name = "device_id", updatable = false)
    @NotNull(message = "device field cannot be null.")
    private Device device;

    @Version
    @Column(name = "entity_version")
    private long entityVersion;

    /**
     * Validates deviceEquipment representation. Returns set of strings which are represent constraint violations. Set
     * will be empty if no constraint violations found.
     *
     * @param deviceEquipment DeviceEquipment that should be validated
     * @param validator       Validator
     * @return Set of strings which are represent constraint violations
     */
    public static Set<String> validate(DeviceEquipment deviceEquipment, Validator validator) {
        Set<ConstraintViolation<DeviceEquipment>> constraintViolations = validator.validate(deviceEquipment);
        Set<String> result = new HashSet<>();
        if (constraintViolations.size() > 0) {
            for (ConstraintViolation<DeviceEquipment> cv : constraintViolations)
                result.add(String.format("Error! property: [%s], value: [%s], message: [%s]",
                        cv.getPropertyPath(), cv.getInvalidValue(), cv.getMessage()));
        }
        return result;

    }

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

    public Timestamp getTimestamp() {
        return ObjectUtils.cloneIfPossible(timestamp);
    }

    public void setTimestamp(Timestamp timestamp) {
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
}
