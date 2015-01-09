package com.devicehive.service;

import com.devicehive.configuration.ConfigurationService;
import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.configuration.PropertiesService;
import com.devicehive.dao.AccessKeyDAO;
import com.devicehive.dao.AccessKeyPermissionDAO;
import com.devicehive.dao.DeviceDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.*;
import com.devicehive.model.enums.AccessType;
import com.devicehive.model.updates.AccessKeyUpdate;
import com.devicehive.service.helpers.AccessKeyProcessor;
import com.devicehive.service.helpers.OAuthAuthenticationUtils;
import com.devicehive.util.LogExecutionTime;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

@Stateless
@LogExecutionTime
@EJB(beanInterface = AccessKeyService.class, name = "AccessKeyService")
public class AccessKeyService {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AccessKeyService.class);

    @EJB
    private AccessKeyDAO accessKeyDAO;
    @EJB
    private AccessKeyPermissionDAO permissionDAO;
    @EJB
    private UserService userService;
    @EJB
    private DeviceDAO deviceDAO;
    @EJB
    private AccessKeyService self;
    @EJB
    private IdentityProviderService identityProviderService;
    @EJB
    private TimestampService timestampService;
    @EJB
    private NetworkService networkService;
    @EJB
    private DeviceService deviceService;
    @EJB
    private ConfigurationService configurationService;
    @EJB
    private PropertiesService propertiesService;
    @EJB
    OAuthAuthenticationUtils authenticationUtils;

    public AccessKey create(@NotNull User user, @NotNull AccessKey accessKey) {
        if (accessKey.getLabel() == null) {
            throw new HiveException(Messages.LABEL_IS_REQUIRED, Response.Status.BAD_REQUEST.getStatusCode());
        }
        if (accessKeyDAO.get(user.getId(), accessKey.getLabel()) != null) {
            throw new HiveException(Messages.DUPLICATE_LABEL_FOUND,
                    Response.Status.BAD_REQUEST.getStatusCode());
        }
        if (accessKey.getId() != null) {
            throw new HiveException(Messages.INVALID_REQUEST_PARAMETERS,
                                    Response.Status.BAD_REQUEST.getStatusCode());
        }
        authenticationUtils.validateActions(accessKey);
        AccessKeyProcessor keyProcessor = new AccessKeyProcessor();
        String key = keyProcessor.generateKey();
        accessKey.setKey(key);
        accessKey.setUser(user);
        accessKeyDAO.insert(accessKey);
        for (AccessKeyPermission current : accessKey.getPermissions()) {
            AccessKeyPermission permission = preparePermission(current);
            permission.setAccessKey(accessKey);
            permissionDAO.insert(permission);
        }
        return accessKey;
    }

    public boolean update(@NotNull Long userId, @NotNull Long keyId, AccessKeyUpdate toUpdate) {
        AccessKey existing = accessKeyDAO.get(userId, keyId);
        if (existing == null) {
            return false;
        }
        if (toUpdate == null) {
            return true;
        }
        if (toUpdate.getLabel() != null) {
            existing.setLabel(toUpdate.getLabel().getValue());
        }
        if (toUpdate.getExpirationDate() != null) {
            existing.setExpirationDate(toUpdate.getExpirationDate().getValue());
        }
        if (toUpdate.getPermissions() != null) {
            Set<AccessKeyPermission> permissionsToReplace = toUpdate.getPermissions().getValue();
            if (permissionsToReplace == null) {
                throw new HiveException(Messages.INVALID_REQUEST_PARAMETERS,
                                        Response.Status.BAD_REQUEST.getStatusCode());
            }
            AccessKey toValidate = toUpdate.convertTo();
            authenticationUtils.validateActions(toValidate);
            permissionDAO.deleteByAccessKey(existing);
            for (AccessKeyPermission current : permissionsToReplace) {
                AccessKeyPermission permission = preparePermission(current);
                permission.setAccessKey(existing);
                permissionDAO.insert(permission);
            }
        }
        return true;
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public AccessKey authenticate(@NotNull String key) {
        return accessKeyDAO.get(key);
    }

    public AccessKey exchangeCode(@NotNull String code, @NotNull IdentityProvider identityProvider) {
        final Long githubProviderId = Long.parseLong(propertiesService.getProperty(Constants.GITHUB_IDENTITY_PROVIDER_ID));
        if (githubProviderId.equals(identityProvider.getId())) {
            final String githubAccessToken = getGithubAccessToken(code);
            if (githubAccessToken != null) {
                return authenticate(githubAccessToken, identityProvider);
            }
        }
        return null;
    }

    public AccessKey authenticate(@NotNull String accessToken, @NotNull IdentityProvider identityProvider) {
        if (identityProvider.getVerificationEndpoint() != null) {
            final JsonElement verificationResponse =  executeGet(new NetHttpTransport(),
                    BearerToken.queryParameterAccessMethod(), accessToken, identityProvider.getVerificationEndpoint(), identityProvider.getName());
            if (!authenticationUtils.validateVerificationResponse(verificationResponse.getAsJsonObject(), identityProvider)) {
                throw new HiveException(String.format(Messages.OAUTH_ACCESS_TOKEN_VERIFICATION_FAILED, identityProvider.getName()),
                        Response.Status.FORBIDDEN.getStatusCode());
            }
        }
        final JsonElement apiResponse =  executeGet(new NetHttpTransport(), BearerToken.authorizationHeaderAccessMethod(),
                    accessToken, identityProvider.getApiEndpoint(), identityProvider.getName());
        final String email = authenticationUtils.getLoginFromResponse(apiResponse, identityProvider.getId());
        User user = userService.findByLoginAndIdentity(email, identityProvider);
        if (user == null) {
            LOGGER.error("No user with email {} found for identity provider {}", email, identityProvider.getName());
            throw new HiveException(String.format(Messages.USER_NOT_FOUND, email),
                    Response.Status.NOT_FOUND.getStatusCode());
        }
        LOGGER.debug("User authentication success: {}", user.getLogin());
        userService.refreshUserLoginData(user);
        AccessKey accessKey = accessKeyDAO.get(user.getId(),
                String.format(OAuthAuthenticationUtils.OAUTH_ACCESS_KEY_LABEL_FORMAT, email));
        if (accessKey == null) {
            LOGGER.debug("No access key found for user {}. A new one will be created");
            return createExternalAccessToken(user, email);
        }
        if (accessKey.getExpirationDate().before(new Timestamp(System.currentTimeMillis()))) {
            LOGGER.debug("Access key has expired");
            delete(null, accessKey.getId());
            return createExternalAccessToken(user, email);
        }
        return accessKey;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    private AccessKey createExternalAccessToken(final User user, final String email) {
        AccessKey accessKey = authenticationUtils.prepareAccessKey(user, email);

        Set<AccessKeyPermission> permissions = new HashSet<>();
        final AccessKeyPermission permission = authenticationUtils.preparePermission(user.getRole());
        permissions.add(permission);
        accessKey.setPermissions(permissions);
        accessKeyDAO.insert(accessKey);

        permission.setAccessKey(accessKey);
        permissionDAO.insert(permission);
        return accessKey;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public AccessKey find(@NotNull Long keyId, @NotNull Long userId) {
        return accessKeyDAO.get(userId, keyId);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public boolean hasAccessToNetwork(AccessKey accessKey, Network targetNetwork) {
        Set<AccessKeyPermission> permissions = accessKey.getPermissions();
        Set<Long> allowedNetworks = new HashSet<>();
        User user = accessKey.getUser();
        Set<AccessKeyPermission> toRemove = new HashSet<>();
        for (AccessKeyPermission currentPermission : permissions) {
            if (currentPermission.getNetworkIdsAsSet() == null) {
                allowedNetworks.add(null);
            } else {
                if (currentPermission.getNetworkIdsAsSet().contains(targetNetwork.getId())) {
                    allowedNetworks.addAll(currentPermission.getNetworkIdsAsSet());
                } else {
                    toRemove.add(currentPermission);
                }
            }
        }
        permissions.removeAll(toRemove);
        if (allowedNetworks.contains(null)) {
            return userService.hasAccessToNetwork(user, targetNetwork);
        }
        user = userService.findUserWithNetworks(user.getId());
        return allowedNetworks.contains(targetNetwork.getId()) &&
               (user.isAdmin() || user.getNetworks().contains(targetNetwork));
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public boolean hasAccessToDevice(AccessKey accessKey, String deviceGuid) {
        Set<AccessKeyPermission> permissions = accessKey.getPermissions();
        Set<String> allowedDevices = new HashSet<>();
        Set<Long> allowedNetworks = new HashSet<>();

        User accessKeyUser = userService.findUserWithNetworks(accessKey.getUser().getId());
        Set<AccessKeyPermission> toRemove = new HashSet<>();

        Device device = deviceDAO.findByUUIDWithNetworkAndDeviceClass(deviceGuid);      //not good way

        for (AccessKeyPermission currentPermission : permissions) {
            if (currentPermission.getDeviceGuidsAsSet() == null) {
                allowedDevices.add(null);
            } else {
                if (!currentPermission.getDeviceGuidsAsSet().contains(deviceGuid)) {
                    toRemove.add(currentPermission);
                } else {
                    allowedDevices.addAll(currentPermission.getDeviceGuidsAsSet());
                }
            }
            if (currentPermission.getNetworkIdsAsSet() == null) {
                allowedNetworks.add(null);
            } else {
                if (device.getNetwork() != null) {
                    if (!currentPermission.getNetworkIdsAsSet().contains(device.getNetwork().getId())) {
                        toRemove.add(currentPermission);
                    } else {
                        allowedNetworks.addAll(currentPermission.getNetworkIdsAsSet());
                    }
                }
            }
        }
        permissions.removeAll(toRemove);
        boolean hasAccess;
        hasAccess = allowedDevices.contains(null) ?
                    userService.hasAccessToDevice(accessKeyUser, device.getGuid()) :
                    allowedDevices.contains(device.getGuid()) && userService.hasAccessToDevice(accessKeyUser, device.getGuid());

        hasAccess = hasAccess && allowedNetworks.contains(null) ?
                    accessKeyUser.isAdmin() || accessKeyUser.getNetworks().contains(device.getNetwork()) :
                    (accessKeyUser.isAdmin() || accessKeyUser.getNetworks().contains(device.getNetwork()))
                    && allowedNetworks.contains(device.getNetwork().getId());

        return hasAccess;
    }

    public AccessKey createAccessKeyFromOAuthGrant(OAuthGrant grant, User user, Timestamp now) {
        AccessKey newKey = new AccessKey();
        if (grant.getAccessType().equals(AccessType.ONLINE)) {
            Timestamp expirationDate = new Timestamp(now.getTime() + 600000);  //the key is valid for 10 minutes
            newKey.setExpirationDate(expirationDate);
        }
        newKey.setUser(user);
        newKey.setLabel(String.format(Messages.OAUTH_TOKEN_LABEL, grant.getClient().getName()));
        Set<AccessKeyPermission> permissions = new HashSet<>();
        AccessKeyPermission permission = new AccessKeyPermission();
        permission.setDomains(grant.getClient().getDomain());
        permission.setActions(StringUtils.split(grant.getScope(), ' '));
        permission.setSubnets(grant.getClient().getSubnet());
        permission.setNetworkIds(grant.getNetworkIds());
        permissions.add(permission);
        newKey.setPermissions(permissions);
        self.create(user, newKey);
        return newKey;
    }

    public AccessKey updateAccessKeyFromOAuthGrant(OAuthGrant grant, User user, Timestamp now) {
        AccessKey existing = self.get(user.getId(), grant.getAccessKey().getId());
        permissionDAO.deleteByAccessKey(existing);
        if (grant.getAccessType().equals(AccessType.ONLINE)) {
            Timestamp expirationDate = new Timestamp(now.getTime() + 600000);  //the key is valid for 10 minutes
            existing.setExpirationDate(expirationDate);
        } else {
            existing.setExpirationDate(null);
        }
        existing.setLabel(String.format(Messages.OAUTH_TOKEN_LABEL, grant.getClient().getName()));
        Set<AccessKeyPermission> permissions = new HashSet<>();
        AccessKeyPermission permission = new AccessKeyPermission();
        permission.setDomains(grant.getClient().getDomain());
        permission.setActions(StringUtils.split(grant.getScope(), ' '));
        permission.setSubnets(grant.getClient().getSubnet());
        permission.setNetworkIds(grant.getNetworkIds());
        permissions.add(permission);
        existing.setPermissions(permissions);
        AccessKeyProcessor keyProcessor = new AccessKeyProcessor();
        String key = keyProcessor.generateKey();
        existing.setKey(key);
        for (AccessKeyPermission current : permissions) {
            current.setAccessKey(existing);
            permissionDAO.insert(current);
        }
        return existing;
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public List<AccessKey> list(@NotNull Long userId) {
        return accessKeyDAO.list(userId);
    }

    public AccessKey get(@NotNull Long userId, @NotNull Long keyId) {
        return accessKeyDAO.get(userId, keyId);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public boolean delete(Long userId, @NotNull Long keyId) {
        if (userId == null) {
            return accessKeyDAO.delete(keyId);
        }
        return accessKeyDAO.delete(userId, keyId);
    }

    public IdentityProvider getIdentityProvider(final String state) {
        return authenticationUtils.getIdentityProvider(state);
    }

    public boolean isIdentityProviderAllowed(@NotNull final IdentityProvider identityProvider) {
        final String identityProviderIdStr = String.valueOf(identityProvider.getId());
        if (identityProviderIdStr.equals(propertiesService.getProperty(Constants.GOOGLE_IDENTITY_PROVIDER_ID))) {
            return Boolean.valueOf(configurationService.get(Constants.GOOGLE_IDENTITY_ALLOWED));
        } else if (identityProviderIdStr.equals(propertiesService.getProperty(Constants.FACEBOOK_IDENTITY_PROVIDER_ID))) {
            return Boolean.valueOf(configurationService.get(Constants.FACEBOOK_IDENTITY_ALLOWED));
        } else if (identityProviderIdStr.equals(propertiesService.getProperty(Constants.GITHUB_IDENTITY_PROVIDER_ID))) {
            return Boolean.valueOf(configurationService.get(Constants.GITHUB_IDENTITY_ALLOWED));
        } else
            throw new HiveException(String.format(Messages.IDENTITY_PROVIDER_NOT_FOUND, identityProviderIdStr), BAD_REQUEST.getStatusCode());
    }

    private String getGithubAccessToken(final String code) {
        final String endpoint = propertiesService.getProperty(Constants.GITHUB_IDENTITY_ACCESS_TOKEN_ENDPOINT);
        Map<String, String> params = new HashMap(4);
        params.put("code", code);
        params.put("client_id", configurationService.get(Constants.GITHUB_IDENTITY_CLIENT_ID));
        params.put("client_secret", configurationService.get(Constants.GITHUB_IDENTITY_CLIENT_SECRET));
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept("application/json");
        final JsonObject response = executePost(new NetHttpTransport(), params, headers, endpoint, "Github");
        if (response.get("access_token") != null) {
            return response.get("access_token").getAsString();
        } else {
            LOGGER.warn("No access token found in provider response: {}", response);
            throw new HiveException(Messages.BAD_AUTHENTICATION_RESPONSE, Response.Status.BAD_REQUEST.getStatusCode());
        }
    }

    private JsonElement executeGet(final HttpTransport transport, final Credential.AccessMethod accessMethod, final String accessToken,
                                           final String endpoint, final String providerName) {
        LOGGER.debug("executeGet: endpoint {}, providerName {}", endpoint, providerName);
        try {
            final Credential credential = new Credential(accessMethod).setAccessToken(accessToken);
            final GenericUrl url = new GenericUrl(endpoint);
            final HttpRequestFactory requestFactory = transport.createRequestFactory(credential);
            final String response = requestFactory.buildGetRequest(url).execute().parseAsString();
            JsonElement jsonElement = new JsonParser().parse(response);
            LOGGER.debug("executeGet response: {}", jsonElement);
            try {
                final JsonElement error = jsonElement.getAsJsonObject().get("error");
                if (error != null) {
                    LOGGER.error("Exception has been caught during Identity Provider GET request execution", error);
                    throw new HiveException(String.format(Messages.OAUTH_ACCESS_TOKEN_VERIFICATION_FAILED, providerName, error),
                            Response.Status.FORBIDDEN.getStatusCode());
                }
            } catch (IllegalStateException ex) {
                return jsonElement;
            }
            return jsonElement;
        } catch (IOException e) {
            LOGGER.error("Exception has been caught during Identity Provider GET request execution", e);
            throw new HiveException(Messages.IDENTITY_PROVIDER_API_REQUEST_ERROR, Response.Status.SERVICE_UNAVAILABLE.getStatusCode());
        }
    }

    private JsonObject executePost(final HttpTransport transport, final Map<String, String> params, final HttpHeaders headers,
                                     final String endpoint, final String providerName) {
        LOGGER.debug("executePost: endpoint {}, providerName {}", endpoint, providerName);
        try {
            final HttpRequestFactory requestFactory = transport.createRequestFactory();
            final GenericUrl url = new GenericUrl(endpoint);
            HttpContent httpContent = new UrlEncodedContent(params);
            HttpRequest request = requestFactory.buildPostRequest(url, httpContent);
            request.setHeaders(headers);
            final String response = request.execute().parseAsString();
            final JsonObject jsonObject = new JsonParser().parse(response).getAsJsonObject();
            LOGGER.debug("executeGet response: {}", jsonObject);
            final JsonElement error = jsonObject.get("error");
            if (error != null) {
                LOGGER.error("Exception has been caught during Identity Provider POST request execution, CODE: {}",
                        params.get("code"), error);
                throw new HiveException(String.format(Messages.OAUTH_ACCESS_TOKEN_VERIFICATION_FAILED, providerName, error),
                        Response.Status.FORBIDDEN.getStatusCode());
            }
            return jsonObject;
        } catch (IOException e) {
            LOGGER.error("Exception has been caught during Identity Provider POST request execution", e);
            throw new HiveException(Messages.IDENTITY_PROVIDER_API_REQUEST_ERROR, Response.Status.SERVICE_UNAVAILABLE.getStatusCode());
        }
    }

    private AccessKeyPermission preparePermission(AccessKeyPermission current) {
        AccessKeyPermission newPermission = new AccessKeyPermission();
        if (current.getDomainsAsSet() != null) {
            newPermission.setDomains(current.getDomains());
        }
        if (current.getSubnetsAsSet() != null) {
            newPermission.setSubnets(current.getSubnets());
        }
        if (current.getActionsAsSet() != null) {
            newPermission.setActions(current.getActions());
        }
        if (current.getNetworkIdsAsSet() != null) {
            newPermission.setNetworkIds(current.getNetworkIds());
        }
        if (current.getDeviceGuidsAsSet() != null) {
            newPermission.setDeviceGuids(current.getDeviceGuids());
        }
        return newPermission;
    }
}
