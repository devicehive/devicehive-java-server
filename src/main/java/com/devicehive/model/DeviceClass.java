package com.devicehive.model;


import com.devicehive.json.strategies.JsonPolicyDef;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Set;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

/**
 * Represents a device class which holds meta-information about devices.
 */
@Entity
@Table(name = "device_class")
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class DeviceClass implements HiveEntity {

    private static final long serialVersionUID = 8091624406245592117L;

    @Id
    @Column(name = "name")
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

    @Version
    @Column(name = "entity_version")
    private Long entityVersion;

    @OneToMany(mappedBy = "deviceClass", fetch = FetchType.LAZY)
    @JsonPolicyDef({DEVICECLASS_PUBLISHED, DEVICE_PUBLISHED})
    private Set<Equipment> equipment;

    public DeviceClass() {
        System.out.println("creation");
    }

    public Long getEntityVersion() {
        return entityVersion;
    }

    public void setEntityVersion(Long entityVersion) {
        this.entityVersion = entityVersion;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DeviceClass that = (DeviceClass) o;

        return !(name != null ? !name.equals(that.name) : that.name != null);

    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    public static class Queries {

        public interface Names {

            String GET_WITH_EQUIPMENT = "DeviceClass.getWithEquipment";
            String DELETE_BY_ID = "DeviceClass.deleteById";
            String GET_ALL = "DeviceClass.getAll";
        }

        interface Values {

            String GET_WITH_EQUIPMENT =
                "select d from DeviceClass d " +
                "left join fetch d.equipment " +
                "where d.id = :id";
            String DELETE_BY_ID = "delete from DeviceClass d where d.id = :id";
            String GET_ALL = "select dc from DeviceClass dc";
        }

        public interface Parameters {

            String NAME = "name";
            String VERSION = "version";
            String ID = "id";
        }
    }
}
