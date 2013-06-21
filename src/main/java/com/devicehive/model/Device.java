package com.devicehive.model;

import com.devicehive.model.converters.JsonConverter;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * TODO JavaDoc
 */


@Entity
@Table(name = "device")
@NamedQueries({
        @NamedQuery(name = "Device.findByUUID", query = "select d from Device d where guid = :uuid"),
        @NamedQuery(name = "Device.findByUUIDAndKey",
                query = "select d from Device d where guid = :uuid and key = :key")
})
public class Device implements Serializable{

    @SerializedName("id")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @SerializedName("guid")
    @Column
    @Type(type = "pg-uuid") //That's hibernate-specific and postgres-specific, ugly
    private UUID guid;

    @SerializedName("key")
    @Column
    @NotNull(message = "key field cannot be null.")
    @Size(min = 1, max = 64, message = "Field cannot be empty. The length of key shouldn't be more than 64 symbols.")
    private String key;

    @SerializedName("name")
    @Column
    @NotNull(message = "name field cannot be null.")
    @Size(min = 1, max = 128, message = "Field cannot be empty. The length of name shouldn't be more than 128 symbols.")
    private String name;

    @SerializedName("status")
    @Column
    @Size(min = 1, max = 128,
            message = "Field cannot be empty. The length of status shouldn't be more than 128 symbols.")
    private String status;

    @SerializedName("data")
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name="jsonString", column=@Column(name = "data"))
    })
    private JsonStringWrapper data;

    @SerializedName("network")
    @ManyToOne
    @JoinColumn(name = "network_id")
    private Network network;

    @SerializedName("deviceClass")
    @ManyToOne
    @JoinColumn(name = "device_class_id")
    @NotNull(message = "deviceClass field cannot be null.")
    private DeviceClass deviceClass;

    public Device() {
    }

    /**
     * Validates device representation. Returns set of strings which are represent constraint violations. Set will be
     * empty if no constraint violations found.
     *
     * @param device    Device that should be validated
     * @param validator Validator
     * @return Set of strings which are represent constraint violations
     */
    public static Set<String> validate(Device device, Validator validator) {
        Set<ConstraintViolation<Device>> constraintViolations = validator.validate(device);
        Set<String> result = new HashSet<>();
        if (constraintViolations.size() > 0) {
            for (ConstraintViolation<Device> cv : constraintViolations)
                result.add(String.format("Error! property: [%s], value: [%s], message: [%s]",
                        cv.getPropertyPath(), cv.getInvalidValue(), cv.getMessage()));
        }
        return result;

    }

    /*
    @SerializedName("equipment")
    @OneToMany
    private List<Equipment> equipment;
    */

    public JsonStringWrapper getData() {
        return data;
    }

    public void setData(JsonStringWrapper data) {
        this.data = data;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getGuid() {
        return guid;
    }

    public void setGuid(UUID guid) {
        this.guid = guid;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Network getNetwork() {
        return network;
    }

    public void setNetwork(Network network) {
        this.network = network;
    }

    public DeviceClass getDeviceClass() {
        return deviceClass;
    }
                  /*
    public List<Equipment> getEquipment() {
        return equipment;
    }

    public void setEquipment(List<Equipment> equipment) {
        this.equipment = equipment;
    }                                            */

    public void setDeviceClass(DeviceClass deviceClass) {
        this.deviceClass = deviceClass;
    }

}