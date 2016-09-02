package com.devicehive.model.rpc;

import com.devicehive.shim.api.Body;

import java.util.Date;
import java.util.Set;

public class CommandSearchRequest extends Body {

    private Long id;
    private String guid;
    private Set<String> names;
    private Date timestampStart;
    private Date timestampEnd;
    private String status;

    public CommandSearchRequest() {
        super(Action.COMMAND_SEARCH_REQUEST.name());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public Set<String> getNames() {
        return names;
    }

    public void setNames(Set<String> names) {
        this.names = names;
    }

    public Date getTimestampStart() {
        return timestampStart;
    }

    public void setTimestampStart(Date timestampStart) {
        this.timestampStart = timestampStart;
    }

    public Date getTimestampEnd() {
        return timestampEnd;
    }

    public void setTimestampEnd(Date timestampEnd) {
        this.timestampEnd = timestampEnd;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

}
