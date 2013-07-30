package com.devicehive.messages.data.subscriptions.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Entity
@Table(indexes = {
        @Index(columnList = "deviceId", unique = true),
        @Index(columnList = "sessionId")
})
@NamedQueries({
        @NamedQuery(name = "CommandsSubscription.deleteBySession", query = "delete from CommandsSubscription c where " +
                "c.sessionId = :sessionId "),
        @NamedQuery(name = "CommandsSubscription.deleteByDevice", query = "delete from CommandsSubscription c where c.deviceId = :deviceId"),
        @NamedQuery(name = "CommandsSubscription.getByDeviceId", query = "select c from CommandsSubscription c where c.deviceId = :deviceId")
})
public class CommandsSubscription {

    @Id
    @GeneratedValue
    private Long id;

    @Column
    @NotNull
    private Long deviceId;

    @Column
    @NotNull
    private String sessionId;

    public CommandsSubscription() {
    }

    public CommandsSubscription(Long deviceId, String sessionId) {
        this.deviceId = deviceId;
        this.sessionId = sessionId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((deviceId == null) ? 0 : deviceId.hashCode());
        result = prime * result + ((sessionId == null) ? 0 : sessionId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CommandsSubscription other = (CommandsSubscription) obj;
        if (deviceId == null) {
            if (other.deviceId != null)
                return false;
        }
        else if (!deviceId.equals(other.deviceId))
            return false;
        if (sessionId == null) {
            if (other.sessionId != null)
                return false;
        }
        else if (!sessionId.equals(other.sessionId))
            return false;
        return true;
    }
}
