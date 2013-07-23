package com.devicehive.websockets.messagebus.local.subscriptions.model;

import javax.persistence.*;
import javax.validation.constraints.NotNull;


@Entity
@Table(indexes = {
        @Index(columnList = "deviceId"),
        @Index(columnList = "sessionId")},
        uniqueConstraints = @UniqueConstraint(columnNames = {"deviceId", "sessionId"}))
@NamedQueries({
        @NamedQuery(name = "NotificationsSubscription.deleteBySession",
                query = "delete from NotificationsSubscription n where n.sessionId = :sessionId "),
        @NamedQuery(name = "NotificationsSubscription.deleteByDevicesAndSession",
                query = "delete from NotificationsSubscription n where n.deviceId in :deviceIdList and n.sessionId = " +
                        ":sessionId"),
        @NamedQuery(name = "NotificationsSubscription.selectAll", query = "select n from NotificationsSubscription n"),
        @NamedQuery(name = "NotificationsSubscription.getSubscribedForAll",
                query = "select n.sessionId from NotificationsSubscription n where n.deviceId is null"),
        @NamedQuery(name = "NotificationsSubscription.getSubscribedByDevice",
                query = "select n.sessionId from NotificationsSubscription n where n.deviceId = :deviceId")
})
public class NotificationsSubscription {

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
}
