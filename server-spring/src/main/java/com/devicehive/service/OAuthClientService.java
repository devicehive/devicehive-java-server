package com.devicehive.service;


import com.devicehive.configuration.Messages;
import com.devicehive.dao.OAuthClientDAO;
import com.devicehive.exceptions.HiveException;
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

import javax.validation.constraints.NotNull;
import java.util.List;

import static javax.servlet.http.HttpServletResponse.*;

@Component
public class OAuthClientService {
    private static final Logger LOGGER = LoggerFactory.getLogger(OAuthClientService.class);

    @Autowired
    private OAuthClientDAO clientDAO;

    private PasswordProcessor secretGenerator = new DefaultPasswordProcessor();

    @Transactional(propagation = Propagation.SUPPORTS)
    public OAuthClient get(@NotNull Long id) {
        return clientDAO.get(id);
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
        return clientDAO.list(name, namePattern, domain, oauthId, sortField, sortOrderAsc, take, skip);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public OAuthClient insert(OAuthClient client) {
        if (client.getId() != null) {
            LOGGER.error("OAuth client id shouldn't be empty");
            throw new HiveException(Messages.ID_NOT_ALLOWED, SC_BAD_REQUEST);
        }
        OAuthClient clientWithExistingID = clientDAO.get(client.getOauthId());
        if (clientWithExistingID != null) {
            LOGGER.error("OAuth client with id {} not found", client.getOauthId());
            throw new HiveException(Messages.DUPLICATE_OAUTH_ID, SC_FORBIDDEN);
        }
        client.setOauthSecret(secretGenerator.generateSalt());
        clientDAO.insert(client);
        return client;
    }

    @Transactional
    public boolean update(OAuthClientUpdate client, Long clientId) {
        OAuthClient existing = clientDAO.get(clientId);
        if (existing == null) {
            LOGGER.error("OAuth client with id {} not found", clientId);
            throw new HiveException(String.format(Messages.OAUTH_CLIENT_NOT_FOUND, clientId), SC_NOT_FOUND);
        }
        if (client == null) {
            return true;
        }
        if (client.getOauthId() != null && !client.getOauthId().getValue().equals(existing.getOauthId())) {
            OAuthClient existingWithOAuthID = clientDAO.get(client.getOauthId().getValue());
            if (existingWithOAuthID != null) {
                LOGGER.error("OAuth client with id {} already exists in the system", client.getOauthId().getValue());
                throw new HiveException(Messages.DUPLICATE_OAUTH_ID, SC_FORBIDDEN);
            }
        }
        if (client.getName() != null) {
            existing.setName(client.getName().getValue());
        }
        if (client.getDomain() != null) {
            existing.setDomain(client.getDomain().getValue());
        }
        if (client.getSubnet() != null) {
            existing.setSubnet(client.getSubnet().getValue());
        }
        if (client.getRedirectUri() != null) {
            existing.setRedirectUri(client.getRedirectUri().getValue());
        }
        if (client.getOauthId() != null) {
            existing.setOauthId(client.getOauthId().getValue());
        }
        return true;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public boolean delete(@NotNull Long id) {
        return clientDAO.delete(id);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public OAuthClient getByOAuthID(String oauthID) {
        return clientDAO.get(oauthID);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public OAuthClient authenticate(@NotNull String id, @NotNull String secret) {
        return clientDAO.get(id, secret);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public OAuthClient getByName(String name) {
        return clientDAO.getByName(name);
    }
}

