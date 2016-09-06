package com.devicehive.model.rpc;

import com.devicehive.shim.api.Body;

public class ListUserRequest extends Body {

    private String login;
    private String loginPattern;
    private Integer role;
    private Integer status;
    private String sortField;
    private Boolean sortOrderAsc;
    private Integer take;
    private Integer skip;

    public ListUserRequest() {
        super(Action.LIST_USER_REQUEST.name());
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getLoginPattern() {
        return loginPattern;
    }

    public void setLoginPattern(String loginPattern) {
        this.loginPattern = loginPattern;
    }

    public Integer getRole() {
        return role;
    }

    public void setRole(Integer role) {
        this.role = role;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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
