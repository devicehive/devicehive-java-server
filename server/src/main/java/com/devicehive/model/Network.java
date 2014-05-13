package com.devicehive.model;

import com.devicehive.json.strategies.JsonPolicyDef;
import com.google.gson.annotations.SerializedName;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.HashSet;
import java.util.Set;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.DEVICE_PUBLISHED;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.DEVICE_PUBLISHED_DEVICE_AUTH;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.DEVICE_SUBMITTED;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.NETWORKS_LISTED;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.NETWORK_PUBLISHED;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.NETWORK_SUBMITTED;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.USER_PUBLISHED;
import static com.devicehive.model.Network.Queries.Names;
import static com.devicehive.model.Network.Queries.Values;

/**
 * TODO JavaDoc
 */
@Entity
@Table(name = "network")
@NamedQueries({
        @NamedQuery(name = Names.FIND_BY_NAME, query = Values.FIND_BY_NAME),
        @NamedQuery(name = Names.FIND_WITH_USERS, query = Values.FIND_WITH_USERS),
        @NamedQuery(name = Names.DELETE_BY_ID, query = Values.DELETE_BY_ID),
        @NamedQuery(name = Names.GET_WITH_DEVICES_AND_DEVICE_CLASSES,
                query = Values.GET_WITH_DEVICES_AND_DEVICE_CLASSES)
})
@Cacheable
public class Network implements HiveEntity {
    public static final String USERS_ASSOCIATION = "users";
    public static final String ID_COLUMN = "id";
    public static final String NAME_COLUMN = "name";
    private static final long serialVersionUID = -4824259625517565076L;
    @SerializedName("id")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonPolicyDef({DEVICE_PUBLISHED_DEVICE_AUTH, DEVICE_PUBLISHED, USER_PUBLISHED, NETWORKS_LISTED,
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

    public static interface Queries {
        public static interface Names {
            public static final String FIND_BY_NAME = "Network.findByName";
            public static final String FIND_WITH_USERS = "Network.findWithUsers";
            public static final String DELETE_BY_ID = "Network.deleteById";
            public static final String GET_WITH_DEVICES_AND_DEVICE_CLASSES = "Network.getWithDevicesAndDeviceClasses";
        }

        static interface Values {
            static final String FIND_BY_NAME = "select n from Network n where name = :name";
            static final String FIND_WITH_USERS = "select n from Network n left join fetch n.users where n.id = :id";
            static final String DELETE_BY_ID = "delete from Network n where n.id = :id";
            static final String GET_WITH_DEVICES_AND_DEVICE_CLASSES =
                    "select n from Network n " +
                            "left join fetch n.devices " +
                            "where n.id = :id";
        }

        public static interface Parameters {
            public static final String NAME = "name";
            public static final String ID = "id";
        }
    }
}
