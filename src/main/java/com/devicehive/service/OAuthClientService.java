package com.devicehive.service;


import com.devicehive.configuration.Messages;
import com.devicehive.dao.CacheConfig;
import com.devicehive.dao.CriteriaHelper;
import com.devicehive.dao.OAuthClientDao;
import com.devicehive.dao.rdbms.OAuthClientDaoImpl;
import com.devicehive.exceptions.ActionNotAllowedException;
import com.devicehive.exceptions.IllegalParametersException;
import com.devicehive.model.OAuthClient;
import com.devicehive.model.updates.OAuthClientUpdate;
import com.devicehive.service.helpers.DefaultPasswordProcessor;
import com.devicehive.service.helpers.PasswordProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static java.util.Optional.ofNullable;

@Component
public class OAuthClientService {
    private static final Logger logger = LoggerFactory.getLogger(OAuthClientService.class);

    @Autowired
    private OAuthClientDao oAuthClientDao;

    private PasswordProcessor secretGenerator = new DefaultPasswordProcessor();

    @Transactional(propagation = Propagation.SUPPORTS)
    public OAuthClient get(@NotNull Long id) {
        return oAuthClientDao.find(id);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<OAuthClient> get(String name,
                                 String namePattern,
                                 String domain,
                                 String oauthId,
                                 String sortField,
                                 Boolean sortOrderAsc,
                                 Integer take,
                                 Integer skip) {
        return oAuthClientDao.get(name,
                namePattern,
                domain,
                oauthId,
                sortField,
                sortOrderAsc,
                take,
                skip);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public OAuthClient insert(OAuthClient client) {
        if (client.getId() != null) {
            logger.error("OAuth client id shouldn't be empty");
            throw new IllegalParametersException(Messages.ID_NOT_ALLOWED);
        }
        OAuthClient clientWithExistingID = getByOAuthID(client.getOauthId());
        if (clientWithExistingID != null) {
            logger.error("OAuth client with id {} not found", client.getOauthId());
            throw new ActionNotAllowedException(Messages.DUPLICATE_OAUTH_ID);
        }
        client.setOauthSecret(secretGenerator.generateSalt());
        oAuthClientDao.persist(client);
        return client;
    }

    @Transactional
    public boolean update(OAuthClientUpdate client, Long clientId) {
        OAuthClient existing = oAuthClientDao.find(clientId);
        if (existing == null) {
            logger.error("OAuth client with id {} not found", clientId);
            throw new NoSuchElementException(String.format(Messages.OAUTH_CLIENT_NOT_FOUND, clientId));
        }
        if (client == null) {
            return true;
        }
        if (client.getOauthId() != null && !client.getOauthId().orElse(null).equals(existing.getOauthId())) {
            OAuthClient existingWithOAuthID = getByOAuthID(client.getOauthId().orElse(null));
            if (existingWithOAuthID != null) {
                logger.error("OAuth client with id {} already exists in the system", client.getOauthId().orElse(null));
                throw new ActionNotAllowedException(Messages.DUPLICATE_OAUTH_ID);
            }
        }
        if (client.getName() != null) {
            existing.setName(client.getName().orElse(null));
        }
        if (client.getDomain() != null) {
            existing.setDomain(client.getDomain().orElse(null));
        }
        if (client.getSubnet() != null) {
            existing.setSubnet(client.getSubnet().orElse(null));
        }
        if (client.getRedirectUri() != null) {
            existing.setRedirectUri(client.getRedirectUri().orElse(null));
        }
        if (client.getOauthId() != null) {
            existing.setOauthId(client.getOauthId().orElse(null));
        }
        oAuthClientDao.merge(existing);
        return true;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public boolean delete(@NotNull Long id) {
        int result = oAuthClientDao.deleteById(id);
        return result > 0;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public OAuthClient getByOAuthID(String oauthID) {
        return oAuthClientDao.getByOAuthId(oauthID);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public OAuthClient authenticate(@NotNull String id, @NotNull String secret) {
        return oAuthClientDao.getByOAuthIdAndSecret(id, secret);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public OAuthClient getByName(String name) {
        return oAuthClientDao.getByName(name);
    }
}

