package com.devicehive.websockets.messagebus.local.subscriptions.model;

import javax.persistence.*;
import javax.validation.constraints.NotNull;


@Entity
@Table(indexes = {
        @Index(columnList = "commandId"),
        @Index( columnList = "sessionId")
})
@NamedQueries({
        @NamedQuery(name = "CommandUpdatesSubscription.deleteBySession", query = "delete from CommandUpdatesSubscription c where c.sessionId = :sessionId ")
})
public class CommandUpdatesSubscription {

    @Id
    @GeneratedValue
    private Long id;


    @Column
    @NotNull
    private Long commandId;


    @Column
    @NotNull
    private String sessionId;


    public CommandUpdatesSubscription() {
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCommandId() {
        return commandId;
    }

    public void setCommandId(Long commandId) {
        this.commandId = commandId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
