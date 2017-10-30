package com.devicehive.model;

import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.vo.DeviceTypeVO;
import com.google.gson.annotations.SerializedName;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Set;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

@Entity
@Table(name = "device_type")
@NamedQueries({
        @NamedQuery(name = "DeviceType.findByName", query = "select t from DeviceType t where t.name = :name"),
        @NamedQuery(name = "DeviceType.findWithUsers", query = "select t from DeviceType t left join fetch t.users where t.id = :id"),
        @NamedQuery(name = "DeviceType.findByUserOrderedById", query = "select t from DeviceType t left join t.users u where u.id = :id order by t.id"),
        @NamedQuery(name = "DeviceType.deleteById", query = "delete from DeviceType t where t.id = :id"),
        @NamedQuery(name = "DeviceType.getWithDevices", query = "select t from DeviceType t left join fetch t.devices where t.id = :id"),
        @NamedQuery(name = "DeviceType.getDeviceTypesByIdsAndUsers", query = "select t from DeviceType t left outer join t.users u left join fetch t.devices d " +
                "where t.id in :deviceTypeIds and (u.id = :userId or :userId is null) and (t.id in :permittedDeviceTypes or :permittedDeviceTypes is null)")
})
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class DeviceType implements HiveEntity {

    private static final long serialVersionUID = -4534503697839217385L;

    @SerializedName("id")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonPolicyDef({DEVICE_PUBLISHED, USER_PUBLISHED, DEVICE_TYPES_LISTED, DEVICE_TYPE_PUBLISHED, DEVICE_TYPE_SUBMITTED})
    private Long id;

    @SerializedName("name")
    @Column
    @NotNull(message = "name field cannot be null.")
    @Size(min = 1, max = 128, message = "Field cannot be empty. The length of name should not be more than 128 " +
            "symbols.")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, USER_PUBLISHED, DEVICE_TYPES_LISTED, DEVICE_TYPE_PUBLISHED})
    private String name;

    @SerializedName("description")
    @Column
    @Size(max = 128, message = "The length of description should not be more than 128 symbols.")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, USER_PUBLISHED, DEVICE_TYPES_LISTED, DEVICE_TYPE_PUBLISHED})
    private String description;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_device_type", joinColumns = {@JoinColumn(name = "device_type_id", nullable = false,
            updatable = false)},
            inverseJoinColumns = {@JoinColumn(name = "user_id", nullable = false, updatable = false)})
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<User> users;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "device_type")
    @JsonPolicyDef({DEVICE_TYPE_PUBLISHED})
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<Device> devices;

    @Version
    @Column(name = "entity_version")
    private Long entityVersion;

    public Set<Device> getDevices() {
        return devices;
    }

    public void setDevices(Set<Device> devices) {
        this.devices = devices;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<User> getUsers() {
        return users;
    }

    public void setUsers(Set<User> users) {
        this.users = users;
    }

    public Long getEntityVersion() {
        return entityVersion;
    }

    public void setEntityVersion(Long entityVersion) {
        this.entityVersion = entityVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DeviceType deviceType = (DeviceType) o;

        return !(id != null ? !id.equals(deviceType.id) : deviceType.id != null);

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "DeviceType{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }

    public static DeviceTypeVO convertDeviceType(DeviceType deviceType) {
        if (deviceType != null) {
            DeviceTypeVO vo = new DeviceTypeVO();
            vo.setId(deviceType.getId());
            vo.setName(deviceType.getName());
            vo.setDescription(deviceType.getDescription());
            vo.setEntityVersion(deviceType.getEntityVersion());
            return vo;
        }
        return null;
    }

    public static DeviceType convert(DeviceTypeVO vo) {
        if (vo != null) {
            DeviceType deviceType = new DeviceType();
            deviceType.setId(vo.getId());
            deviceType.setName(vo.getName());
            deviceType.setDescription(vo.getDescription());
            deviceType.setEntityVersion(vo.getEntityVersion());
            return deviceType;
        }
        return null;
    }
}
