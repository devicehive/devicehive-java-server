package com.devicehive.model.rpc;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.shim.api.Body;

import java.util.Optional;

public class ListNetworkRequest extends Body {

    private String name;
    private String namePattern;
    private String sortField;
    private boolean sortOrderAsc;
    private Integer take;
    private Integer skip;
    private Optional<HivePrincipal> principal;

    public ListNetworkRequest() {
        super(Action.LIST_NETWORK_REQUEST.name());
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

    public boolean getSortOrderAsc() {
        return sortOrderAsc;
    }

    public void setSortOrderAsc(boolean sortOrderAsc) {
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

    public Optional<HivePrincipal> getPrincipal() {
        return principal;
    }

    public void setPrincipal(Optional<HivePrincipal> principal) {
        this.principal = principal;
    }
}
