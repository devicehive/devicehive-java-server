package com.devicehive.model;

import com.google.gson.annotations.SerializedName;

import javax.persistence.*;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;


/**
 * TODO JavaDoc
 */
@Entity
@Table(name = "device_command")
@NamedQueries({
    @NamedQuery(name= "DeviceCommand.getNewerThan", query = "select dc from DeviceCommand dc where dc.timestamp > :timestamp and dc.device = :device"),
})
public class DeviceCommand implements Serializable{

    @SerializedName("id")
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    public Long id;

    @SerializedName("timestamp")
    @Column
    public Date timestamp;

    @SerializedName("user")
    @ManyToOne
    @JoinColumn(name = "user_id", updatable = false)
    public User user;

    @SerializedName("device")
    @ManyToOne
    @JoinColumn(name = "device_id", updatable = false)
    @NotNull(message = "device field cannot be null.")
    public Device device;

    @SerializedName("command")
    @Column
    @NotNull(message = "command field cannot be null.")
    @Size(min = 1, max = 128, message = "Field cannot be empty. The length of command shouldn't be more than 128 " +
            "symbols.")
    public String command;

    @SerializedName("parameters")
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name="jsonString", column=@Column(name = "parameters"))
    })
    public JsonStringWrapper parameters;

    @SerializedName("lifetime")
    @Column
    public Integer lifetime;

    @SerializedName("flags")
    @Column
    public Integer flags;

    @SerializedName("status")
    @Column
    public String status;

    @SerializedName("result")
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name="jsonString", column=@Column(name = "result"))
    })
    public JsonStringWrapper result;

    public DeviceCommand() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
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

    /**
     * Validates deviceCommand representation. Returns set of strings which are represent constraint violations. Set
     * will be empty if no constraint violations found.
     * @param deviceCommand
     * DeviceCommand that should be validated
     * @param validator
     * Validator
     * @return Set of strings which are represent constraint violations
     */
    public static Set<String> validate(DeviceCommand deviceCommand, Validator validator) {
        Set<ConstraintViolation<DeviceCommand>> constraintViolations = validator.validate(deviceCommand);
        Set<String> result = new HashSet<>();
        if (constraintViolations.size()>0){
            for (ConstraintViolation<DeviceCommand> cv : constraintViolations)
                result.add(String.format("Error! property: [%s], value: [%s], message: [%s]",
                        cv.getPropertyPath(), cv.getInvalidValue(), cv.getMessage()));
        }
        return result;

    }
}
