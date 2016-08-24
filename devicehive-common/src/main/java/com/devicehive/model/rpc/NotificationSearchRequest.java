package com.devicehive.model.rpc;

import com.devicehive.shim.api.Body;

import java.util.Date;
import java.util.Set;

//TODO [rafa] That object to be split into two different objects. The first would contain id+guid, the second the rest of the fields.
public class NotificationSearchRequest extends Body {

    private Long id;
    private String guid;
    private Set<String> devices;
    private Set<String> names;
    private Date timestamp;
    private String status;
    private Integer take;
    private Boolean hasResponse;

    public NotificationSearchRequest() {
        super(Action.NOTIFICATION_SEARCH_REQUEST.name());
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

    public Set<String> getDevices() {
        return devices;
    }

    public void setDevices(Set<String> devices) {
        this.devices = devices;
    }

    public Set<String> getNames() {
        return names;
    }

    public void setNames(Set<String> names) {
        this.names = names;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getTake() {
        return take;
    }

    public void setTake(Integer take) {
        this.take = take;
    }

    public Boolean getHasResponse() {
        return hasResponse;
    }

    public void setHasResponse(Boolean hasResponse) {
        this.hasResponse = hasResponse;
    }
}
