package com.devicehive.messages.data.subscriptions.model;

import javax.persistence.*;
import javax.validation.constraints.NotNull;


@Entity
@Table(indexes = {
        @Index(columnList = "commandId", unique = true),
        @Index( columnList = "sessionId")
})
@NamedQueries({
        @NamedQuery(name = "CommandUpdatesSubscription.deleteBySession",
                query = "delete from CommandUpdatesSubscription c where c.sessionId = :sessionId"),
        @NamedQuery(name = "CommandUpdateSubscription.getByCommandId",
                query = "select c from  CommandUpdatesSubscription c where c.commandId = :commandId"),
        @NamedQuery(name = "CommandUpdatesSubscription.deleteByCommandId",
                query = "delete from CommandUpdatesSubscription c where c.commandId = :commandId")
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

    public CommandUpdatesSubscription(Long commandId, String sessionId) {
        this.commandId = commandId;
        this.sessionId = sessionId;
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
