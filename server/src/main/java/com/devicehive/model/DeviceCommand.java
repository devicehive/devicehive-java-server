package com.devicehive.model;

import com.google.gson.annotations.SerializedName;

import com.devicehive.json.strategies.JsonPolicyDef;

import org.apache.commons.lang3.ObjectUtils;

import java.sql.Timestamp;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.COMMAND_FROM_CLIENT;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.COMMAND_LISTED;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.COMMAND_TO_CLIENT;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.COMMAND_TO_DEVICE;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.COMMAND_UPDATE_FROM_DEVICE;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.COMMAND_UPDATE_TO_CLIENT;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.POST_COMMAND_TO_DEVICE;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.REST_COMMAND_UPDATE_FROM_DEVICE;
import static com.devicehive.model.DeviceCommand.Queries.Names;
import static com.devicehive.model.DeviceCommand.Queries.Values;

/**
 * TODO JavaDoc
 */
@Entity
@Table(name = "device_command")
@NamedQueries({
                  @NamedQuery(name = Names.DELETE_BY_ID, query = Values.DELETE_BY_ID),
                  @NamedQuery(name = Names.DELETE_BY_DEVICE_AND_USER, query = Values.DELETE_BY_DEVICE_AND_USER),
                  @NamedQuery(name = Names.DELETE_BY_FOREIGN_KEY, query = Values.DELETE_BY_FOREIGN_KEY),
                  @NamedQuery(name = Names.GET_BY_DEVICE_UUID_AND_ID, query = Values.GET_BY_DEVICE_UUID_AND_ID)
              })
@Cacheable
public class DeviceCommand implements HiveEntity {

    public static final String TIMESTAMP_COLUMN = "timestamp";
    public static final String DEVICE_COLUMN = "device";
    public static final String COMMAND_COLUMN = "command";
    public static final String STATUS_COLUMN = "status";
    public static final String ID_COLUMN = "id";
    private static final long serialVersionUID = -1062670903456135249L;
    @SerializedName("id")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonPolicyDef({COMMAND_TO_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT, POST_COMMAND_TO_DEVICE,
                    COMMAND_LISTED})
    private Long id;
    @SerializedName("timestamp")
    @Column(insertable = false, updatable = false)
    @JsonPolicyDef(
        {COMMAND_TO_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT, POST_COMMAND_TO_DEVICE, COMMAND_LISTED})
    private Timestamp timestamp;
    @SerializedName("user")
    @ManyToOne
    @JoinColumn(name = "user_id", updatable = false)
    private User user;
    @SerializedName("userId")
    @Transient
    @JsonPolicyDef({COMMAND_TO_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT, COMMAND_LISTED})
    private Long userId;
    @SerializedName("device")
    @ManyToOne
    @JoinColumn(name = "device_id", updatable = false)
    @NotNull(message = "device field cannot be null.")
    private Device device;
    @SerializedName("command")
    @Column
    @NotNull(message = "command field cannot be null.")
    @Size(min = 1, max = 128,
          message = "Field cannot be empty. The length of command should not be more than 128 symbols.")
    @JsonPolicyDef({COMMAND_FROM_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT, COMMAND_UPDATE_FROM_DEVICE,
                    POST_COMMAND_TO_DEVICE, COMMAND_LISTED})
    private String command;
    @SerializedName("parameters")
    @Embedded
    @AttributeOverrides({
                            @AttributeOverride(name = "jsonString", column = @Column(name = "parameters"))
                        })
    @JsonPolicyDef({COMMAND_FROM_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT, COMMAND_UPDATE_FROM_DEVICE,
                    POST_COMMAND_TO_DEVICE, COMMAND_LISTED})
    private JsonStringWrapper parameters;
    @SerializedName("lifetime")
    @Column
    @JsonPolicyDef({COMMAND_FROM_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT, COMMAND_UPDATE_FROM_DEVICE,
                    COMMAND_LISTED})
    private Integer lifetime;
    @SerializedName("flags")
    @Column
    @JsonPolicyDef({COMMAND_FROM_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT, COMMAND_UPDATE_FROM_DEVICE,
                    REST_COMMAND_UPDATE_FROM_DEVICE, COMMAND_LISTED})
    private Integer flags;
    @SerializedName("status")
    @Column
    @JsonPolicyDef({COMMAND_FROM_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT, COMMAND_UPDATE_FROM_DEVICE,
                    POST_COMMAND_TO_DEVICE,
                    REST_COMMAND_UPDATE_FROM_DEVICE, COMMAND_LISTED})
    private String status;
    @SerializedName("result")
    @Embedded
    @AttributeOverrides({
                            @AttributeOverride(name = "jsonString", column = @Column(name = "result"))
                        })
    @JsonPolicyDef({COMMAND_FROM_CLIENT, COMMAND_TO_DEVICE, COMMAND_UPDATE_TO_CLIENT, COMMAND_UPDATE_FROM_DEVICE,
                    POST_COMMAND_TO_DEVICE,
                    REST_COMMAND_UPDATE_FROM_DEVICE, COMMAND_LISTED})
    private JsonStringWrapper result;
    @Column(name = "origin_session_id")
    @Size(min = 1, max = 64,
          message = "The length of origin_session_id should not be more than 64 symbols.")
    private String originSessionId;
    @Version
    @Column(name = "entity_version")
    private long entityVersion;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Timestamp getTimestamp() {
        return ObjectUtils.cloneIfPossible(timestamp);
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = ObjectUtils.cloneIfPossible(timestamp);
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public JsonStringWrapper getParameters() {
        return parameters;
    }

    public void setParameters(JsonStringWrapper parameters) {
        this.parameters = parameters;
    }

    public Integer getLifetime() {
        return lifetime;
    }

    public void setLifetime(Integer lifetime) {
        this.lifetime = lifetime;
    }

    public Integer getFlags() {
        return flags;
    }

    public void setFlags(Integer flags) {
        this.flags = flags;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public JsonStringWrapper getResult() {
        return result;
    }

    public void setResult(JsonStringWrapper result) {
        this.result = result;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public long getEntityVersion() {
        return entityVersion;
    }

    public void setEntityVersion(long entityVersion) {
        this.entityVersion = entityVersion;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getOriginSessionId() {
        return originSessionId;
    }

    public void setOriginSessionId(String originSessionId) {
        this.originSessionId = originSessionId;
    }

    public static class Queries {

        public static interface Names {

            static final String DELETE_BY_ID = "DeviceCommand.deleteById";
            static final String DELETE_BY_DEVICE_AND_USER = "DeviceCommand.deleteByDeviceAndUser";
            static final String DELETE_BY_FOREIGN_KEY = "DeviceCommand.deleteByFK";
            static final String GET_BY_DEVICE_UUID_AND_ID = "DeviceCommand.getByDeviceUuidAndId";
        }

        static interface Values {

            static final String DELETE_BY_ID = "delete from DeviceCommand dc where dc.id = :id";
            static final String DELETE_BY_DEVICE_AND_USER =
                "delete from DeviceCommand dc " +
                "where dc.user = :user and dc.device = :device";
            static final String DELETE_BY_FOREIGN_KEY = "delete from DeviceCommand dc where dc.device = :device";
            static final String GET_BY_DEVICE_UUID_AND_ID =
                "select dc from DeviceCommand dc " +
                "where dc.id = :id and dc.device.guid = :guid";
        }

        public static interface Parameters {

            static final String ID = "id";
            static final String USER = "user";
            static final String DEVICE = "device";
            static final String GUID = "guid";
        }
    }
}
