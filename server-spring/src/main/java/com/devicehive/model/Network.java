package com.devicehive.model;

import com.devicehive.json.strategies.JsonPolicyDef;
import com.google.gson.annotations.SerializedName;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Set;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;
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
                              query = Values.GET_WITH_DEVICES_AND_DEVICE_CLASSES),
                  @NamedQuery(name = "Network.getNetworksByIdsAndUsers", query = "select n from Network n left outer join n.users u where n.id in :networkIds and (u.id = :userId or :userId is null)")
              })
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
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

        Network network = (Network) o;

        return !(id != null ? !id.equals(network.id) : network.id != null);

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Network{" +
                "id=" + id +
                ", key='" + key + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }

    public interface Queries {

        interface Names {

            String FIND_BY_NAME = "Network.findByName";
            String FIND_WITH_USERS = "Network.findWithUsers";
            String DELETE_BY_ID = "Network.deleteById";
            String GET_WITH_DEVICES_AND_DEVICE_CLASSES = "Network.getWithDevicesAndDeviceClasses";
        }

        interface Values {

            String FIND_BY_NAME = "select n from Network n where name = :name";
            String FIND_WITH_USERS = "select n from Network n left join fetch n.users where n.id = :id";
            String DELETE_BY_ID = "delete from Network n where n.id = :id";
            String GET_WITH_DEVICES_AND_DEVICE_CLASSES =
                "select n from Network n " +
                "left join fetch n.devices " +
                "where n.id = :id";
        }

        interface Parameters {

            String NAME = "name";
            String ID = "id";
        }
    }
}
