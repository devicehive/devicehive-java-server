package com.devicehive.model;


import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.request.DeviceClassInsert;

import javax.persistence.*;
import javax.persistence.Version;
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
        @NamedQuery(name = "DeviceClass.updateDeviceClassById",
                query = "update DeviceClass d set " +
                        "d.isPermanent = :isPermanent, " +
                        "d.offlineTimeout = :offlineTimeout, " +
                        "d.data = :data " +
                        "where d.id = :id")
})
public class DeviceClass implements HiveEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonPolicyDef({DEVICE_PUBLISHED})
    private Long id;

    @Column
    @NotNull(message = "name field cannot be null.")
    @Size(min = 1, max = 128, message = "Field cannot be empty. The length of name shouldn't be more than 128 symbols.")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED})
    private String name;

    @Column
    @NotNull(message = "name field cannot be null.")
    @Size(min = 1, max = 32, message = "Field cannot be empty. The length of version shouldn't be more than 32 " +
            "symbols.")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED})
    private String version;

    @Column(name = "is_permanent")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED})
    private Boolean isPermanent;

    @Column(name = "offline_timeout")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED})
    private Integer offlineTimeout;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "jsonString", column = @Column(name = "data"))
    })
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED})
    private JsonStringWrapper data;

    @Version
    @Column(name = "entity_version")
    private long entityVersion;

    @OneToMany(mappedBy = "deviceClass")
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

    /**
     * copies fields from {@link DeviceClassInsert}
     * @param insert object, that will be used as data source
     */
    public void copyFieldsFrom(DeviceClassInsert insert) {
        setName(insert.getName());
        setPermanent(insert.getIsPermanent());
        setVersion(insert.getVersion());
        setOfflineTimeout(insert.getOfflineTimeout());
        setData(insert.getData());
    }

    public void copyFieldsFromOmmitingNulls(DeviceClassInsert insert) {
        if(insert.getName()!=null){
            setName(insert.getName());
        }
        if(insert.getIsPermanent()!=null){
            setPermanent(insert.getIsPermanent());
        }
        if(insert.getVersion()!=null){
            setVersion(insert.getVersion());
        }
        if(insert.getOfflineTimeout()!=null){
            setOfflineTimeout(insert.getOfflineTimeout());
        }
        if(insert.getOfflineTimeout()!=null){
            setData(insert.getData());
        }
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
    public boolean equals(Object dc){

        if(dc instanceof DeviceClass) {
            return ((DeviceClass)dc).getId()==getId();
        }

        return false;
    }
}
