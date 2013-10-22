package com.devicehive.model;


import com.devicehive.json.strategies.JsonPolicyDef;

import javax.persistence.*;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.HashSet;
import java.util.Set;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;


/**
 * Represents a device class which holds meta-information about devices.
 */
@Entity
@Table(name = "device_class")
@NamedQueries({
        @NamedQuery(name = "DeviceClass.findByNameAndVersion",
                query = "select d from DeviceClass d where d.name = :name and d.version = :version"),
        @NamedQuery(name = "DeviceClass.list",
                query = "select d from DeviceClass d where d.name = :name and d.version = :version"),
        @NamedQuery(name = "DeviceClass.getWithEquipment",
                query = "select d from DeviceClass d left join fetch d.equipment where d.id = :id"),
        @NamedQuery(name = "DeviceClass.deleteById", query = "delete from DeviceClass d where d.id = :id"),
        @NamedQuery(name = "DeviceClass.getByDevice", query = "select d.deviceClass from Device d where d.guid = :guid")
})
@Cacheable
public class DeviceClass implements HiveEntity {

    private static final long serialVersionUID = 8091624406245592117L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonPolicyDef(
            {DEVICE_PUBLISHED, DEVICE_PUBLISHED_DEVICE_AUTH, NETWORK_PUBLISHED, DEVICECLASS_LISTED, DEVICECLASS_PUBLISHED, DEVICECLASS_SUBMITTED})
    private Long id;

    @Column
    @NotNull(message = "name field cannot be null.")
    @Size(min = 1, max = 128, message = "Field cannot be empty. The length of name should not be more than 128 " +
            "symbols.")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_PUBLISHED_DEVICE_AUTH, DEVICE_SUBMITTED, NETWORK_PUBLISHED, DEVICECLASS_LISTED, DEVICECLASS_PUBLISHED})
    private String name;

    @Column
    @NotNull(message = "name field cannot be null.")
    @Size(min = 1, max = 32, message = "Field cannot be empty. The length of version should not be more than 32 " +
            "symbols.")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_PUBLISHED_DEVICE_AUTH, DEVICE_SUBMITTED, NETWORK_PUBLISHED, DEVICECLASS_LISTED, DEVICECLASS_PUBLISHED})
    private String version;

    @Column(name = "is_permanent")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_PUBLISHED_DEVICE_AUTH, DEVICE_SUBMITTED, NETWORK_PUBLISHED, DEVICECLASS_LISTED, DEVICECLASS_PUBLISHED})
    private Boolean isPermanent;

    @Column(name = "offline_timeout")
    @JsonPolicyDef({DEVICE_PUBLISHED,DEVICE_PUBLISHED_DEVICE_AUTH, DEVICE_SUBMITTED, NETWORK_PUBLISHED, DEVICECLASS_LISTED, DEVICECLASS_PUBLISHED})
    private Integer offlineTimeout;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "jsonString", column = @Column(name = "data"))
    })
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_PUBLISHED_DEVICE_AUTH, DEVICE_SUBMITTED, NETWORK_PUBLISHED, DEVICECLASS_LISTED, DEVICECLASS_PUBLISHED})
    private JsonStringWrapper data;

    @Version
    @Column(name = "entity_version")
    private long entityVersion;

    @OneToMany(mappedBy = "deviceClass", fetch = FetchType.LAZY)
    @JsonPolicyDef({DEVICECLASS_PUBLISHED, DEVICE_PUBLISHED_DEVICE_AUTH, DEVICE_PUBLISHED})
    private Set<Equipment> equipment;

    /**
     * Validates deviceClass representation. Returns set of strings which are represent constraint violations. Set will
     * be empty if no constraint violations found.
     *
     * @param deviceClass DeviceClass that should be validated
     * @param validator   Validator
     * @return Set of strings which are represent constraint violations
     */
    public static Set<String> validate(DeviceClass deviceClass, Validator validator) {
        Set<ConstraintViolation<DeviceClass>> constraintViolations = validator.validate(deviceClass);
        Set<String> result = new HashSet<>();
        if (constraintViolations.size() > 0) {
            for (ConstraintViolation<DeviceClass> cv : constraintViolations)
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
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

    public Set<Equipment> getEquipment() {
        return equipment;
    }

    public void setEquipment(Set<Equipment> equipment) {
        this.equipment = equipment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DeviceClass that = (DeviceClass) o;

        return !(id != null ? !id.equals(that.id) : that.id != null);

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
