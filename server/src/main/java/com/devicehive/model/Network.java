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
@Table(name = "network")
@NamedQueries({
        @NamedQuery(name = "Network.findByName", query = "select n from Network n where name = :name"),
        @NamedQuery(name = "Network.findWithUsers",
                query = "select n from Network n left join fetch n.users where n.id = :id"),
        @NamedQuery(name = "Network.deleteById", query = "delete from Network n where n.id = :id"),
        @NamedQuery(name = "Network.getWithDevicesAndDeviceClasses", query = "select n from Network n " +
                "left join fetch n.devices where n.id = :id"),
        @NamedQuery(name = "Network.getWithDevicesAndDeviceClassesForUser", query = "select n from Network n " +
                "left join fetch n.devices inner join fetch n.users u where u.id = :userId and n.id = :id"),
        @NamedQuery(name = "Network.getByDevice", query = "select d.network from Device d where d.guid = :guid")
})
@Cacheable
public class Network implements HiveEntity {

    private static final long serialVersionUID = -4824259625517565076L;
    @SerializedName("id")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonPolicyDef({DEVICE_PUBLISHED_DEVICE_AUTH,DEVICE_PUBLISHED, USER_PUBLISHED, NETWORKS_LISTED,
            NETWORK_PUBLISHED, NETWORK_SUBMITTED})
    private Long id;

    @SerializedName("key")
    @Column
    @Size(max = 64, message = "The length of key should not be more than 64 symbols.")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, USER_PUBLISHED, NETWORKS_LISTED, NETWORK_PUBLISHED})
    private String key;

    @SerializedName("name")
    @Column
    @NotNull(message = "name field cannot be null.")
    @Size(min = 1, max = 128, message = "Field cannot be empty. The length of name should not be more than 128 " +
            "symbols.")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_PUBLISHED_DEVICE_AUTH, DEVICE_SUBMITTED, USER_PUBLISHED,
            NETWORKS_LISTED, NETWORK_PUBLISHED})
    private String name;

    @SerializedName("description")
    @Column
    @Size(max = 128, message = "The length of description should not be more than 128 symbols.")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_PUBLISHED_DEVICE_AUTH, DEVICE_SUBMITTED, USER_PUBLISHED,
            NETWORKS_LISTED, NETWORK_PUBLISHED})
    private String description;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_network", joinColumns = {@JoinColumn(name = "network_id", nullable = false,
            updatable = false)},
            inverseJoinColumns = {@JoinColumn(name = "user_id", nullable = false, updatable = false)})
    private Set<User> users;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "network")
    @JsonPolicyDef({NETWORK_PUBLISHED})
    private Set<Device> devices;

    @Version
    @Column(name = "entity_version")
    private long entityVersion;

    /**
     * Validates network representation. Returns set of strings which are represent constraint violations. Set will
     * be empty if no constraint violations found.
     *
     * @param network   Network that should be validated
     * @param validator Validator
     * @return Set of strings which are represent constraint violations
     */
    public static Set<String> validate(Network network, Validator validator) {
        Set<ConstraintViolation<Network>> constraintViolations = validator.validate(network);
        Set<String> result = new HashSet<>();
        if (!constraintViolations.isEmpty()) {
            for (ConstraintViolation<Network> cv : constraintViolations)
                result.add(String.format("Error! property: [%s], value: [%s], message: [%s]",
                        cv.getPropertyPath(), cv.getInvalidValue(), cv.getMessage()));
        }
        return result;
    }

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

    public long getEntityVersion() {
        return entityVersion;
    }

    public void setEntityVersion(long entityVersion) {
        this.entityVersion = entityVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Network network = (Network) o;

        if (id != null ? !id.equals(network.id) : network.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
