package com.devicehive.model.rpc;

import com.devicehive.shim.api.Body;

public class ListDeviceClassRequest extends Body {

    private String name;
    private String namePattern;
    private String sortField;
    private Boolean sortOrderAsc;
    private Integer take;
    private Integer skip;

    public ListDeviceClassRequest() {
        super(Action.LIST_DEVICE_CLASS_REQUEST.name());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNamePattern() {
        return namePattern;
    }

    public void setNamePattern(String namePattern) {
        this.namePattern = namePattern;
    }

    public String getSortField() {
        return sortField;
    }

    public void setSortField(String sortField) {
        this.sortField = sortField;
    }

    public Boolean getSortOrderAsc() {
        return sortOrderAsc;
    }

    public void setSortOrderAsc(Boolean sortOrderAsc) {
        this.sortOrderAsc = sortOrderAsc;
    }

    public Integer getTake() {
        return take;
    }

    public void setTake(Integer take) {
        this.take = take;
    }

    public Integer getSkip() {
        return skip;
    }

    public void setSkip(Integer skip) {
        this.skip = skip;
    }
}
