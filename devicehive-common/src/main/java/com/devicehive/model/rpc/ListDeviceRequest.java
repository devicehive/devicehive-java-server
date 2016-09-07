package com.devicehive.model.rpc;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.shim.api.Body;

public class ListDeviceRequest extends Body {

    private String name;
    private String namePattern;
    private String status;
    private Long networkId;
    private String networkName;
    private Long deviceClassId;
    private String deviceClassName;
    private String sortField;
    private Boolean sortOrderAsc;
    private Integer take;
    private Integer skip;
    private HivePrincipal principal;

    public ListDeviceRequest() {
        super(Action.LIST_DEVICE_REQUEST.name());
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getNetworkId() {
        return networkId;
    }

    public void setNetworkId(Long networkId) {
        this.networkId = networkId;
    }

    public String getNetworkName() {
        return networkName;
    }

    public void setNetworkName(String networkName) {
        this.networkName = networkName;
    }

    public Long getDeviceClassId() {
        return deviceClassId;
    }

    public void setDeviceClassId(Long deviceClassId) {
        this.deviceClassId = deviceClassId;
    }

    public String getDeviceClassName() {
        return deviceClassName;
    }

    public void setDeviceClassName(String deviceClassName) {
        this.deviceClassName = deviceClassName;
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

    public HivePrincipal getPrincipal() {
        return principal;
    }

    public void setPrincipal(HivePrincipal principal) {
        this.principal = principal;
    }
}
