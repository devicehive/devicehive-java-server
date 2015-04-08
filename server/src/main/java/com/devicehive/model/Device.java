package com.devicehive.model;

import com.devicehive.json.strategies.JsonPolicyDef;
import com.google.gson.annotations.SerializedName;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;
import static com.devicehive.model.Device.Queries.Names;
import static com.devicehive.model.Device.Queries.Values;

/**
 * TODO JavaDoc
 */


@Entity
@Table(name = "device")
@NamedQueries({
                  @NamedQuery(name = Names.FIND_BY_UUID_WITH_NETWORK_AND_DEVICE_CLASS,
                              query = Values.FIND_BY_UUID_WITH_NETWORK_AND_DEVICE_CLASS),
                  @NamedQuery(name = Names.FIND_BY_UUID_AND_KEY, query = Values.FIND_BY_UUID_AND_KEY),
                  @NamedQuery(name = Names.DELETE_BY_UUID, query = Values.DELETE_BY_UUID)
              })
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Device implements HiveEntity {

    public static final String NETWORK_COLUMN = "network";
    public static final String GUID_COLUMN = "guid";

    private static final long serialVersionUID = 2959997451631843298L;
    @Id
    @SerializedName("sid")//overwork for  "declares multiple JSON fields" exception
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @SerializedName("id")
    @Column
    @NotNull(message = "guid field cannot be null.")
    @Size(min = 1, max = 48,
          message = "Field cannot be empty. The length of guid should not be more than 48 symbols.")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_PUBLISHED_DEVICE_AUTH, NETWORK_PUBLISHED})
    private String guid;
    @SerializedName("key")
    @Column
    @NotNull(message = "key field cannot be null.")
    @Size(min = 1, max = 64, message = "Field cannot be empty. The length of key should not be more than 64 symbols.")
    private String key;
    @SerializedName("name")
    @Column
    @NotNull(message = "name field cannot be null.")
    @Size(min = 1, max = 128, message = "Field cannot be empty. The length of name should not be more than 128 " +
                                        "symbols.")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_PUBLISHED_DEVICE_AUTH, DEVICE_SUBMITTED, NETWORK_PUBLISHED})
    private String name;
    @SerializedName("status")
    @Column
    @Size(min = 1, max = 128,
          message = "Field cannot be empty. The length of status should not be more than 128 symbols.")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_PUBLISHED_DEVICE_AUTH, DEVICE_SUBMITTED, NETWORK_PUBLISHED})
    private String status;
    @SerializedName("data")
    @Embedded
    @AttributeOverrides({
                            @AttributeOverride(name = "jsonString", column = @Column(name = "data"))
                        })
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_PUBLISHED_DEVICE_AUTH, DEVICE_SUBMITTED, NETWORK_PUBLISHED})
    private JsonStringWrapper data;
    @SerializedName("network")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "network_id")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_PUBLISHED_DEVICE_AUTH, DEVICE_SUBMITTED})
    private Network network;
    @SerializedName("deviceClass")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "device_class_id")
    @NotNull(message = "deviceClass field cannot be null.")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_PUBLISHED_DEVICE_AUTH, DEVICE_SUBMITTED, NETWORK_PUBLISHED})
    private DeviceClass deviceClass;
    @Version
    @Column(name = "entity_version")
    private long entityVersion;

    public long getEntityVersion() {
        return entityVersion;
    }

    public void setEntityVersion(long entityVersion) {
        this.entityVersion = entityVersion;
    }

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

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
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

    public void setDeviceClass(DeviceClass deviceClass) {
        this.deviceClass = deviceClass;
    }

    public static class Queries {

        public static interface Names {

            static final String FIND_BY_UUID_WITH_NETWORK_AND_DEVICE_CLASS =
                "Device.findByUUIDWithNetworkAndDeviceClass";
            static final String FIND_BY_UUID_AND_KEY = "Device.findByUUIDAndKey";
            static final String DELETE_BY_UUID = "Device.deleteByUUID";
        }

        static interface Values {

            static final String FIND_BY_UUID_WITH_NETWORK_AND_DEVICE_CLASS =
                "select d from Device d " +
                "left join fetch d.network " +
                "left join fetch d.deviceClass dc " +
                "left join fetch dc.equipment " +
                "where d.guid = :guid";
            static final String FIND_BY_UUID_AND_KEY = "select d from Device d where d.guid = :guid and d.key = :key";
            static final String DELETE_BY_UUID = "delete from Device d where d.guid = :guid";
        }

        public static interface Parameters {

            static final String GUID = "guid";
            static final String KEY = "key";
            static final String ID = "id";
        }
    }
}