package com.devicehive.model;


import com.google.gson.JsonElement;
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
@Table(name = "device_notification")
public class DeviceNotification implements Serializable {

    @SerializedName("id")
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @SerializedName("timestamp")
    @Column
    private Date timestamp;

    @SerializedName("notification")
    @Column
    @NotNull(message = "notification field cannot be null.")
    @Size(min = 1, max = 128, message = "Field cannot be empty. The length of notification shouldn't be more than " +
            "128 symbols.")
    private String notification;

    @SerializedName("parameters")
    @Column
    @Convert(converter = com.devicehive.model.converters.Converter.class)
    public JsonElement parameters;

    @ManyToOne
    @JoinColumn
    @NotNull(message = "device field cannot be null.")
    private Device device;

    public DeviceNotification() {
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

    public String getNotification() {
        return notification;
    }

    public void setNotification(String notification) {
        this.notification = notification;
    }

    public JsonElement getParameters() {
        return parameters;
    }

    public void setParameters(JsonElement parameters) {
        this.parameters = parameters;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    /**
     * Validates deviceNotification representation. Returns set of strings which are represent constraint violations.
     * Set
     * will be empty if no constraint violations found.
     * @param deviceNotification
     * DeviceCommand that should be validated
     * @param validator
     * Validator
     * @return Set of strings which are represent constraint violations
     */
    public static Set<String> validate(DeviceNotification deviceNotification, Validator validator) {
        Set<ConstraintViolation<DeviceNotification>> constraintViolations = validator.validate(deviceNotification);
        Set<String> result = new HashSet<>();
        if (constraintViolations.size()>0){
            for (ConstraintViolation<DeviceNotification> cv : constraintViolations)
                result.add(String.format("Error! property: [%s], value: [%s], message: [%s]",
                        cv.getPropertyPath(), cv.getInvalidValue(), cv.getMessage()));
        }
        return result;

    }
}
