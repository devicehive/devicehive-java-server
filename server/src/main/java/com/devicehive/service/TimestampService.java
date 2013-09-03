package com.devicehive.service;


import com.devicehive.configuration.Constants;
import com.devicehive.model.domain.ServerTimestamp;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.sql.Timestamp;

@Stateless
public class TimestampService {

    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;


    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Timestamp getTimestamp() {
        TypedQuery<ServerTimestamp> query = em.createNamedQuery("ServerTimestamp.get", ServerTimestamp.class);
        return query.getSingleResult().getTimestamp();
    }

}
