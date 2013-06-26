package com.devicehive.websockets.messagebus.local.subscriptions.model;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

/**
 * Created with IntelliJ IDEA.
 * User: stas
 * Date: 25.06.13
 * Time: 23:56
 * To change this template use File | Settings | File Templates.
 */

@Entity
@Table(indexes = {
        @Index(columnList = "deviceId"),
        @Index(columnList = "sessionId")
})
@NamedQueries({
        @NamedQuery(name = "CommandsSubscription.deleteBySession", query = "delete from CommandsSubscription c where c.sessionId = :sessionId ")
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
