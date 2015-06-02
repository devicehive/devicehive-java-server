package com.devicehive.service;

import com.devicehive.configuration.Constants;
import com.devicehive.model.ServerTimestamp;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.sql.Timestamp;

import static com.devicehive.model.ServerTimestamp.Queries.Names.GET;

@Profile({"!test"})
@Component
public class DatabaseTimestampService implements TimestampService {

    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Timestamp getTimestamp() {
        TypedQuery<ServerTimestamp> query = em.createNamedQuery(GET, ServerTimestamp.class);
        return query.getSingleResult().getTimestamp();
    }

}
