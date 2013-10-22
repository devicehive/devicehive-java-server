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
@Table(name = "device")
@NamedQueries({
        @NamedQuery(name = "Device.findByUUIDWithNetworkAndDeviceClass", query = "select d from Device d " +
                "left join fetch d.network " +
                "left join fetch d.deviceClass dc " +
                "left join fetch dc.equipment" +
                " where d.guid = :uuid"),
        @NamedQuery(name = "Device.findByUUIDAndKey",
                query = "select d from Device d where d.guid = :uuid and d.key = :key"),
        @NamedQuery(name = "Device.findByUUIDAndUserAndTimestamp",
                query = "select distinct d from DeviceNotification dn " +
                        "inner join dn.device d " +
                        "inner join d.network.users u " +
                        "where dn.timestamp > :timestamp " +
                        "and d.guid in :guidList " +
                        "and u = :user"),
        @NamedQuery(name = "Device.findByNetwork",
                query = "select d from Device d join d.network where d.network = :network"),
        @NamedQuery(name = "Device.findByUUIDListAndNetwork", query = "select d from Device d join d.network " +
                "where d.network = :network and d.guid in :guidList"),
        @NamedQuery(name = "Device.deleteById", query = "delete from Device d where d.id = :id"),
        @NamedQuery(name = "Device.deleteByUUID", query = "delete from Device d where d.guid = :guid"),
        @NamedQuery(name = "Device.deleteByNetwork", query = "delete from Device d where d.network = :network")
})
@Cacheable
public class Device implements HiveEntity {


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
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "network_id")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_PUBLISHED_DEVICE_AUTH, DEVICE_SUBMITTED})
    private Network network;

    @SerializedName("deviceClass")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_class_id")
    @NotNull(message = "deviceClass field cannot be null.")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_PUBLISHED_DEVICE_AUTH, DEVICE_SUBMITTED, NETWORK_PUBLISHED})
    private DeviceClass deviceClass;

    @Version
    @Column(name = "entity_version")
    private long entityVersion;

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

}