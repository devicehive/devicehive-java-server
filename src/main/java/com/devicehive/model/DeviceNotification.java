package com.devicehive.model;


import static com.devicehive.json.strategies.JsonPolicyDef.Policy.NOTIFICATION_FROM_DEVICE;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.NOTIFICATION_TO_CLIENT;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.NOTIFICATION_TO_DEVICE;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Version;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

import com.devicehive.json.strategies.JsonPolicyDef;
import com.google.gson.annotations.SerializedName;

/**
 * TODO JavaDoc
 */
@Entity
@Table(name = "device_notification")
@NamedQueries(value = {
        @NamedQuery(name = "DeviceNotification.getByDeviceListNewerThan",
                query = "select dn from DeviceNotification dn where dn.device.id in :deviceIdList and dn.timestamp > :timestamp"),
        @NamedQuery(name = "DeviceNotification.getByUserNewerThan",
                query = "select dn from DeviceNotification dn where dn.device.id in (" +
                        " select distinct d.id from Device d join d.network n join n.users u where u = :user" +
                        ") and dn.timestamp > :timestamp"),
        @NamedQuery(name = "DeviceNotification.getByDeviceNewerThan",
                query = "select dn from DeviceNotification dn where dn.device in :deviceList and dn.timestamp > " +
                        ":timestamp order by dn.timestamp"),
        @NamedQuery(name = "DeviceNotification.getByNewerThan", query = "select dn from DeviceNotification dn " +
                "where dn.timestamp > :timestamp order by dn.timestamp"),
        @NamedQuery(name = "DeviceNotification.updateById", query = "update DeviceNotification dn set " +
                "dn.parameters = :parameters, " +
                "dn.timestamp = :timestamp, " +
                "dn.notification = :notification " +
                "where id = :id"),
        @NamedQuery(name = "DeviceNotification.deleteById",
                query = "delete from DeviceNotification dn where dn.id = :id"),
        @NamedQuery(name = "DeviceNotification.deleteByFK",
                query = "delete from DeviceNotification dn where dn.device = :device")

})
public class DeviceNotification implements Message, HiveEntity {

    @SerializedName("parameters")
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "jsonString", column = @Column(name = "parameters"))
    })
    @JsonPolicyDef({NOTIFICATION_TO_CLIENT, NOTIFICATION_FROM_DEVICE})
    private JsonStringWrapper parameters;

    @SerializedName("id")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonPolicyDef({NOTIFICATION_TO_CLIENT, NOTIFICATION_TO_DEVICE})
    private Long id;

    @SerializedName("timestamp")
    @Column(insertable = false, updatable = false)
    @JsonPolicyDef({NOTIFICATION_TO_CLIENT, NOTIFICATION_TO_DEVICE})
    private Timestamp timestamp;

    @SerializedName("notification")
    @Column
    @NotNull(message = "notification field cannot be null.")
    @Size(min = 1, max = 128, message = "Field cannot be empty. The length of notification shouldn't be more than " +
            "128 symbols.")
    @JsonPolicyDef({NOTIFICATION_TO_CLIENT, NOTIFICATION_FROM_DEVICE})
    private String notification;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "device_id", updatable = false)
    @NotNull(message = "device field cannot be null.")
    @JsonPolicyDef(NOTIFICATION_TO_CLIENT)
    private Device device;

    @Version
    @Column(name = "entity_version")
    private long entityVersion;

    public DeviceNotification() {
    }

    /**
     * Validates deviceNotification representation. Returns set of strings which are represent constraint violations.
     * Set
     * will be empty if no constraint violations found.
     *
     * @param deviceNotification DeviceCommand that should be validated
     * @param validator          Validator
     * @return Set of strings which are represent constraint violations
     */
    public static Set<String> validate(DeviceNotification deviceNotification, Validator validator) {
        Set<ConstraintViolation<DeviceNotification>> constraintViolations = validator.validate(deviceNotification);
        Set<String> result = new HashSet<>();
        if (constraintViolations.size() > 0) {
            for (ConstraintViolation<DeviceNotification> cv : constraintViolations)
                result.add(String.format("Error! property: [%s], value: [%s], message: [%s]",
                        cv.getPropertyPath(), cv.getInvalidValue(), cv.getMessage()));
        }
        return result;

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getEntityVersion() {
        return entityVersion;
    }

    public void setEntityVersion(long entityVersion) {
        this.entityVersion = entityVersion;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getNotification() {
        return notification;
    }

    public void setNotification(String notification) {
        this.notification = notification;
    }

    public JsonStringWrapper getParameters() {
        return parameters;
    }

    public void setParameters(JsonStringWrapper parameters) {
        this.parameters = parameters;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }
}
