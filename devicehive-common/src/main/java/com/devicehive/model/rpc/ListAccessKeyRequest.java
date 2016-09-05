package com.devicehive.model.rpc;

import com.devicehive.shim.api.Body;

public class ListAccessKeyRequest extends Body {

    private Long userId;
    private String label;
    private String labelPattern;
    private Integer type;
    private String sortField;
    private Boolean sortOrderAsc;
    private Integer take;
    private Integer skip;

    public ListAccessKeyRequest() {
        super(Action.LIST_ACCESS_KEY_REQUEST.name());
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLabelPattern() {
        return labelPattern;
    }

    public void setLabelPattern(String labelPattern) {
        this.labelPattern = labelPattern;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
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
