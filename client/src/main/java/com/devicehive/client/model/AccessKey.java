package com.devicehive.client.model;


import java.sql.Timestamp;
import java.util.Set;

public class AccessKey implements HiveEntity {
    private static final long serialVersionUID = 5031432598347474481L;

    private Long id;
    private NullableWrapper<String> label;
    private NullableWrapper<Timestamp> expirationDate;
    private NullableWrapper<Set<AccessKeyPermission>> permissions;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLabel() {
        return NullableWrapper.value(label);
    }

    public void setLabel(String label) {
        this.label = NullableWrapper.create(label);
    }

    public void removeLabel(String label) {
        this.label = null;
    }

    public Timestamp getExpirationDate() {
        return NullableWrapper.value(expirationDate);
    }

    public void setExpirationDate(Timestamp expirationDate) {
        this.expirationDate = NullableWrapper.create(expirationDate);
    }

    public Set<AccessKeyPermission> getPermissions() {
        return NullableWrapper.value(permissions);
    }

    public void setPermissions(Set<AccessKeyPermission> permissions) {
        this.permissions = NullableWrapper.create(permissions);
    }

    public void removePermissions() {
        this.permissions = null;
    }
}
