package com.devicehive.domain;

import com.devicehive.domain.wrappers.DeviceCommandWrapper;
import org.springframework.cassandra.core.Ordering;
import org.springframework.cassandra.core.PrimaryKeyType;
import org.springframework.data.cassandra.mapping.Column;
import org.springframework.data.cassandra.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.mapping.Table;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by tmatvienko on 2/13/15.
 */
@Table(value = "device_command")
public class DeviceCommand implements Serializable {

    @PrimaryKeyColumn(name = "id", ordinal = 1, type = PrimaryKeyType.PARTITIONED, ordering = Ordering.DESCENDING)
    private String id;

    @PrimaryKeyColumn(name = "device_guid", ordinal = 0, type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING)
    private String deviceGuid;

    private Date timestamp;

    private String command;

    private String parameters;

    private String userId;

    private Integer lifetime;

    private Integer flags;

    private String status;

    private String result;

    @Column("updated")
    private Boolean isUpdated;

    public DeviceCommand() {
    }

    public DeviceCommand(String id, String deviceGuid, Date timestamp, String command, String parameters, String userId, Integer lifetime, Integer flags, String status, String result, Boolean isUpdated) {
        this.id = id;
        this.deviceGuid = deviceGuid;
        this.timestamp = timestamp;
        this.command = command;
        this.parameters = parameters;
        this.userId = userId;
        this.lifetime = lifetime;
        this.flags = flags;
        this.status = status;
        this.result = result;
        this.isUpdated = isUpdated;
    }

    public DeviceCommand(DeviceCommandWrapper wrapper) {
        if (wrapper.getId() != null) {
            this.id = wrapper.getId().toString();
        }
        if (wrapper.getDeviceGuid() != null) {
            this.deviceGuid = wrapper.getDeviceGuid();
        }
        if (wrapper.getTimestamp() != null) {
            this.timestamp = wrapper.getTimestamp();
        }
        if (wrapper.getCommand() != null) {
            this.command = wrapper.getCommand();
        }
        if (wrapper.getParameters() != null) {
            this.parameters = wrapper.getParameters().getJsonString();
        }
        if (wrapper.getUserId() != null) {
            this.userId = wrapper.getUserId().toString();
        }
        if (wrapper.getLifetime() != null) {
            this.lifetime = wrapper.getLifetime();
        }
        if (wrapper.getFlags() != null) {
            this.flags = wrapper.getFlags();
        }
        if (wrapper.getStatus() != null) {
            this.status = wrapper.getStatus();
        }
        if (wrapper.getResult() != null) {
            this.result = wrapper.getResult().getJsonString();
        }
    }

    public String getId() {
        return id;
    }

    public String getDeviceGuid() {
        return deviceGuid;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getCommand() {
        return command;
    }

    public String getParameters() {
        return parameters;
    }

    public String getUserId() {
        return userId;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setDeviceGuid(String deviceGuid) {
        this.deviceGuid = deviceGuid;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setLifetime(Integer lifetime) {
        this.lifetime = lifetime;
    }

    public void setFlags(Integer flags) {
        this.flags = flags;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public Integer getLifetime() {
        return lifetime;
    }

    public Integer getFlags() {
        return flags;
    }

    public String getStatus() {
        return status;
    }

    public String getResult() {
        return result;
    }

    public Boolean getIsUpdated() {
        return isUpdated;
    }

    public void setIsUpdated(Boolean isUpdated) {
        this.isUpdated = isUpdated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DeviceCommand command1 = (DeviceCommand) o;

        if (command != null ? !command.equals(command1.command) : command1.command != null) return false;
        if (deviceGuid != null ? !deviceGuid.equals(command1.deviceGuid) : command1.deviceGuid != null) return false;
        if (flags != null ? !flags.equals(command1.flags) : command1.flags != null) return false;
        if (id != null ? !id.equals(command1.id) : command1.id != null) return false;
        if (isUpdated != null ? !isUpdated.equals(command1.isUpdated) : command1.isUpdated != null) return false;
        if (lifetime != null ? !lifetime.equals(command1.lifetime) : command1.lifetime != null) return false;
        if (parameters != null ? !parameters.equals(command1.parameters) : command1.parameters != null) return false;
        if (result != null ? !result.equals(command1.result) : command1.result != null) return false;
        if (status != null ? !status.equals(command1.status) : command1.status != null) return false;
        if (timestamp != null ? !timestamp.equals(command1.timestamp) : command1.timestamp != null) return false;
        if (userId != null ? !userId.equals(command1.userId) : command1.userId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result1 = id != null ? id.hashCode() : 0;
        result1 = 31 * result1 + (deviceGuid != null ? deviceGuid.hashCode() : 0);
        result1 = 31 * result1 + (timestamp != null ? timestamp.hashCode() : 0);
        result1 = 31 * result1 + (command != null ? command.hashCode() : 0);
        result1 = 31 * result1 + (parameters != null ? parameters.hashCode() : 0);
        result1 = 31 * result1 + (userId != null ? userId.hashCode() : 0);
        result1 = 31 * result1 + (lifetime != null ? lifetime.hashCode() : 0);
        result1 = 31 * result1 + (flags != null ? flags.hashCode() : 0);
        result1 = 31 * result1 + (status != null ? status.hashCode() : 0);
        result1 = 31 * result1 + (result != null ? result.hashCode() : 0);
        result1 = 31 * result1 + (isUpdated != null ? isUpdated.hashCode() : 0);
        return result1;
    }

    @Override
    public String toString() {
        return "DeviceCommand{" +
                "id='" + id + '\'' +
                ", deviceGuid='" + deviceGuid + '\'' +
                ", timestamp=" + timestamp +
                ", command='" + command + '\'' +
                ", parameters='" + parameters + '\'' +
                ", userId='" + userId + '\'' +
                ", lifetime=" + lifetime +
                ", flags=" + flags +
                ", status='" + status + '\'' +
                ", result='" + result + '\'' +
                ", isUpdated=" + isUpdated +
                '}';
    }
}
