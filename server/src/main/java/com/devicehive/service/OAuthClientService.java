package com.devicehive.service;


import com.devicehive.dao.OAuthClientDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.OAuthClient;
import com.devicehive.model.updates.OAuthClientUpdate;
import com.devicehive.service.helpers.DefaultPasswordProcessor;
import com.devicehive.service.helpers.PasswordProcessor;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.validation.constraints.NotNull;
import java.util.List;

import static javax.servlet.http.HttpServletResponse.*;

@Stateless
@EJB(beanInterface = OAuthClientService.class, name = "OAuthClientService")
public class OAuthClientService {
    @EJB
    private OAuthClientDAO clientDAO;
    private PasswordProcessor secretGenerator = new DefaultPasswordProcessor();

    public OAuthClient get(@NotNull Long id) {
        return clientDAO.get(id);
    }

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

    public OAuthClient insert(OAuthClient client) {
        if (client.getId() != null) {
            throw new HiveException("Invalid request. Id cannot be specified.", SC_BAD_REQUEST);
        }
        OAuthClient clientWithExistingID = clientDAO.get(client.getOauthId());
        if (clientWithExistingID != null) {
            throw new HiveException("OAuth client with such OAuthID already exists!", SC_FORBIDDEN);
        }
        client.setOauthSecret(secretGenerator.generateSalt());
        clientDAO.insert(client);
        return client;
    }

    public boolean update(OAuthClientUpdate client, Long clientId) {
        OAuthClient existing = clientDAO.get(clientId);
        if (existing == null) {
            throw new HiveException("OAuth client not found!", SC_NOT_FOUND);
        }
        if (client == null) {
            return true;
        }
        if (client.getOauthId() != null && !client.getOauthId().getValue().equals(existing.getOauthId())) {
            OAuthClient existingWithOAuthID = clientDAO.get(client.getOauthId().getValue());
            if (existingWithOAuthID != null) {
                throw new HiveException("OAuth client with such OAuthID already exists!", SC_FORBIDDEN);
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

    public boolean delete(@NotNull Long id) {
        return clientDAO.delete(id);
    }

    public OAuthClient getByOAuthID(String oauthID){
        return clientDAO.get(oauthID);
    }

    public OAuthClient authenticate(@NotNull String id, @NotNull String secret){
        return clientDAO.get(id, secret);
    }

    public OAuthClient getByName(String name) {
        return clientDAO.getByName(name);
    }
}

