package com.devicehive.service;

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
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    PropertiesService propertiesService;
    @EJB
    OAuthAuthenticationUtils authenticationUtils;

    public AccessKey create(@NotNull User user, @NotNull AccessKey accessKey) {
        if (accessKey.getLabel() == null) {
            throw new HiveException(Messages.LABEL_IS_REQUIRED, Response.Status.BAD_REQUEST.getStatusCode());
        }
        if (accessKey.getId() != null || accessKey.getPermissions() == null || accessKey.getPermissions().isEmpty()) {
            throw new HiveException(Messages.INVALID_REQUEST_PARAMETERS,
                                    Response.Status.BAD_REQUEST.getStatusCode());
        }
        authenticationUtils.validateActions(accessKey);
        AccessKeyProcessor keyProcessor = new AccessKeyProcessor();
        String key = keyProcessor.generateKey();
        accessKey.setKey(key);
        accessKey.setUser(user);
        accessKeyDAO.insert(accessKey);
        for (AccessKeyPermission permission : accessKey.getPermissions()) {
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
                current.setAccessKey(existing);
                permissionDAO.insert(current);
            }
        }
        return true;
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public AccessKey authenticate(@NotNull String key) {
        return accessKeyDAO.get(key);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public AccessKey authenticate(@NotNull String accessToken, @NotNull String state) {
        final IdentityProvider identityProvider = authenticationUtils.getIdentityProvider(state);
        String apiResponse = null;
        try {
            final String verificationResponse =  executeGet(new NetHttpTransport(),
                    BearerToken.queryParameterAccessMethod(), accessToken, identityProvider.getVerificationEndpoint(), identityProvider.getName());
            if (!authenticationUtils.validateVerificationResponse(verificationResponse, identityProvider)) {
                throw new HiveException(String.format(Messages.OAUTH_ACCESS_TOKEN_VERIFICATION_FAILED, identityProvider.getName()),
                        Response.Status.FORBIDDEN.getStatusCode());
            }
            apiResponse =  executeGet(new NetHttpTransport(), BearerToken.authorizationHeaderAccessMethod(),
                    accessToken, identityProvider.getApiEndpoint(), identityProvider.getName());
        } catch (IOException e) {
            LOGGER.error("Exception has been caught during Identity Provider GET request execution", e);
            return null;
        }
        final String email = authenticationUtils.getEmailFromResponse(apiResponse, identityProvider.getId());
        User user = userService.findByLoginAndIdentity(email, identityProvider);
        if (user == null) {
            user = userService.createExternalUser(email, identityProvider);
        }
        AccessKey accessKey = accessKeyDAO.get(user.getId(),
                String.format(OAuthAuthenticationUtils.OAUTH_ACCESS_KEY_LABEL_FORMAT, identityProvider.getName(), email));
        if (accessKey == null) {
            return createExternalAccessToken(user, identityProvider.getName(), email);
        }
        return accessKey;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    private AccessKey createExternalAccessToken(final User user, final String providerName, final String email) {
        AccessKey accessKey = authenticationUtils.prepareAccessKey(user, providerName, email);

        Set<AccessKeyPermission> permissions = new HashSet<>();
        final AccessKeyPermission permission = authenticationUtils.preparePermission();
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
                    userService.hasAccessToDevice(accessKeyUser, device) :
                    allowedDevices.contains(device.getGuid()) && userService.hasAccessToDevice(accessKeyUser, device);

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

    private static String executeGet(final HttpTransport transport, final Credential.AccessMethod accessMethod, final String accessToken,
                                           final String endpoint, final String providerName) throws IOException {
        final Credential credential = new Credential(accessMethod).setAccessToken(accessToken);
        final GenericUrl url = new GenericUrl(endpoint);
        final HttpRequestFactory requestFactory = transport.createRequestFactory(credential);
        final String response = requestFactory.buildGetRequest(url).execute().parseAsString();
        final JsonObject jsonObject = new JsonParser().parse(response).getAsJsonObject();
        if (jsonObject.get("error") != null) {
            throw new HiveException(String.format(Messages.OAUTH_ACCESS_TOKEN_VERIFICATION_FAILED, providerName),
                    Response.Status.FORBIDDEN.getStatusCode());
        }
        return response;
    }
}
