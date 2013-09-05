package com.devicehive.model.view;


import java.sql.Timestamp;
import java.util.Set;

public class AccessKeyView implements HiveEntity {
    private static final long serialVersionUID = 5031432598347474481L;

    private Long id;
    private String label;
    private Timestamp expirationDate;
    private Set<AccessKeyPermissionView> permissions;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Timestamp getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Timestamp expirationDate) {
        this.expirationDate = expirationDate;
    }

    public Set<AccessKeyPermissionView> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<AccessKeyPermissionView> permissions) {
        this.permissions = permissions;
    }
}
