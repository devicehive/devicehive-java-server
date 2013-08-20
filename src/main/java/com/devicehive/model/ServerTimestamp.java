package com.devicehive.model;

import org.apache.commons.lang3.ObjectUtils;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "get_timestamp")
@NamedQueries({
        @NamedQuery(name = "ServerTimestamp.get", query = "select st from ServerTimestamp st")
})
@Cacheable(false)
public class ServerTimestamp {

    @Id
    @Column(insertable = false, updatable = false)
    private Timestamp timestamp;


    public Timestamp getTimestamp() {
        return ObjectUtils.cloneIfPossible(timestamp);
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = ObjectUtils.cloneIfPossible(timestamp);
    }
}
