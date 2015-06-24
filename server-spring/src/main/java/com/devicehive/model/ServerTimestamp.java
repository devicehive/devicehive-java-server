package com.devicehive.model;

import org.apache.commons.lang3.ObjectUtils;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

import static com.devicehive.model.ServerTimestamp.Queries.Names;
import static com.devicehive.model.ServerTimestamp.Queries.Values;

@Entity
@Table(name = "get_timestamp")
@NamedQueries({
                  @NamedQuery(name = Names.GET, query = Values.GET)
              })
@Cacheable(false)
public class ServerTimestamp implements Serializable {

    private static final long serialVersionUID = -4305976280467184553L;
    @Id
    @Column(insertable = false, updatable = false)
    private Timestamp timestamp;

    public Timestamp getTimestamp() {
        return ObjectUtils.cloneIfPossible(timestamp);
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = ObjectUtils.cloneIfPossible(timestamp);
    }

    public static class Queries {

        public static interface Names {

            static final String GET = "ServerTimestamp.get";
        }

        static interface Values {

            static final String GET = "select st from ServerTimestamp st";
        }
    }
}
