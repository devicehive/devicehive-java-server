package com.devicehive.model;

import com.devicehive.json.strategies.JsonPolicyDef;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.ObjectUtils;

import javax.persistence.*;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

/**
 * TODO JavaDoc
 */
@Entity
@Table(name = "device_command")
@NamedQueries({
        @NamedQuery(name = "DeviceCommand.deleteById", query = "delete from DeviceCommand dc where dc.id = :id"),
        @NamedQuery(name = "DeviceCommand.deleteByDeviceAndUser", query = "delete from DeviceCommand dc where dc.user" +
                " = :user and dc.device = :device"),
        @NamedQuery(name = "DeviceCommand.deleteByFK",
                query = "delete from DeviceCommand dc where dc.device = :device"),
        @NamedQuery(name = "DeviceCommand.getByDeviceUuidAndId",
                query = "select dc from DeviceCommand dc where dc.id = :id and dc.device.guid = :guid")
})
@Cacheable
public class DeviceCommand implements HiveEntity {
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
    @JsonPolicyDef({COMMAND_FROM_CLIENT, COMMAND_UPDATE_TO_CLIENT, COMMAND_UPDATE_FROM_DEVICE, COMMAND_LISTED})
    private Integer lifetime;
    @SerializedName("flags")
    @Column
    @JsonPolicyDef({COMMAND_FROM_CLIENT, COMMAND_UPDATE_TO_CLIENT, COMMAND_UPDATE_FROM_DEVICE,
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
    @Version
    @Column(name = "entity_version")
    private long entityVersion;

    /**
     * Validates deviceCommand representation. Returns set of strings which are represent constraint violations. Set
     * will be empty if no constraint violations found.
     *
     * @param deviceCommand DeviceCommand that should be validated
     * @param validator     Validator
     * @return Set of strings which are represent constraint violations
     */
    public static Set<String> validate(DeviceCommand deviceCommand, Validator validator) {
        Set<ConstraintViolation<DeviceCommand>> constraintViolations = validator.validate(deviceCommand);
        Set<String> result = new HashSet<>();
        if (constraintViolations.size() > 0) {
            for (ConstraintViolation<DeviceCommand> cv : constraintViolations) {
                result.add(String.format("Error! property: [%s], value: [%s], message: [%s]",
                        cv.getPropertyPath(), cv.getInvalidValue(), cv.getMessage()));
            }
        }
        return result;

    }

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
}
