package com.devicehive.model;

import org.apache.commons.lang3.ObjectUtils;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.sql.Timestamp;

@Entity
@Table(name = "access_key")
public class AccessKey implements HiveEntity {

    private static final long serialVersionUID = -2630830507065170824L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    @NotNull(message = "Label column cannot be null")
    @Size(min = 1, max = 64,
            message = "Field cannot be empty. The length of guid should not be more than 48 symbols.")
    private String label;

    @Column
    @NotNull(message = "Key column cannot be null")
    @Size(min = 1, max = 48,
            message = "Field cannot be empty. The length of guid should not be more than 48 symbols.")
    private String key;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @NotNull(message = "User column cannot be null")
    private User user;

    @Column(name = "expiration_date")
    private Timestamp expirationDate;

    @Version
    @Column(name = "entity_version")
    private long entityVersion;

    public long getEntityVersion() {
        return entityVersion;
    }

    public void setEntityVersion(long entityVersion) {
        this.entityVersion = entityVersion;
    }

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

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Timestamp getExpirationDate() {
        return ObjectUtils.cloneIfPossible(expirationDate);
    }

    public void setExpirationDate(Timestamp expirationDate) {
        this.expirationDate = ObjectUtils.cloneIfPossible(expirationDate);
    }
}
