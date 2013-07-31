package com.devicehive.messages.data.subscriptions.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

@Entity
@Table(indexes = {
        @Index(columnList = "deviceId"),
        @Index(columnList = "sessionId") },
        uniqueConstraints = @UniqueConstraint(columnNames = { "deviceId", "sessionId" }))
@NamedQueries({
        @NamedQuery(name = "NotificationsSubscription.deleteBySession",
                query = "delete from NotificationsSubscription n where n.sessionId = :sessionId "),
        @NamedQuery(name = "NotificationsSubscription.deleteByDevicesAndSession",
                query = "delete from NotificationsSubscription n where n.deviceId in :deviceIdList and n.sessionId = " +
                        ":sessionId"),
        @NamedQuery(name = "NotificationsSubscription.deleteByDevice", query = "delete from NotificationsSubscription n where n.deviceId = :deviceId"),
        @NamedQuery(name = "NotificationsSubscription.selectAll", query = "select n from NotificationsSubscription n"),
        @NamedQuery(name = "NotificationsSubscription.getSubscribedForAll",
                query = "select n.sessionId from NotificationsSubscription n where n.deviceId is null"),
        @NamedQuery(name = "NotificationsSubscription.getSubscribedByDevice",
                query = "select n.sessionId from NotificationsSubscription n where n.deviceId = :deviceId")
})
public class NotificationsSubscription implements Serializable{

    /** */
    private static final long serialVersionUID = -6361773345693051443L;
    
    @Id
    @GeneratedValue
    private Long id;
    @Column
    //may be null
    private Long deviceId;
    @Column
    @NotNull
    private String sessionId;

    public NotificationsSubscription() {
    }

    public NotificationsSubscription(Long deviceId, String sessionId) {
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
        NotificationsSubscription other = (NotificationsSubscription) obj;
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

    @Override
    public String toString() {
        return "NotificationsSubscription [id=" + id + ", deviceId=" + deviceId + ", sessionId=" + sessionId + "]";
    }

}
