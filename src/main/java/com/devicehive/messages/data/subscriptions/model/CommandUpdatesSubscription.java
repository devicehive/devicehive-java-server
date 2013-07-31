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
import javax.validation.constraints.NotNull;

@Entity
@Table(indexes = {
        @Index(columnList = "commandId", unique = true),
        @Index(columnList = "sessionId")
})
@NamedQueries({
        @NamedQuery(name = "CommandUpdatesSubscription.deleteBySession",
                query = "delete from CommandUpdatesSubscription c where c.sessionId = :sessionId"),
        @NamedQuery(name = "CommandUpdateSubscription.getByCommandId",
                query = "select c from  CommandUpdatesSubscription c where c.commandId = :commandId"),
        @NamedQuery(name = "CommandUpdatesSubscription.deleteByCommandId",
                query = "delete from CommandUpdatesSubscription c where c.commandId = :commandId")
})
public class CommandUpdatesSubscription implements Serializable{

    /** */
    private static final long serialVersionUID = -2473408531678699159L;

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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((commandId == null) ? 0 : commandId.hashCode());
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
        CommandUpdatesSubscription other = (CommandUpdatesSubscription) obj;
        if (commandId == null) {
            if (other.commandId != null)
                return false;
        }
        else if (!commandId.equals(other.commandId))
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
        return "CommandUpdatesSubscription [id=" + id + ", commandId=" + commandId + ", sessionId=" + sessionId + "]";
    }
}
