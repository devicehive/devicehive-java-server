package com.devicehive.service;

import com.devicehive.configuration.ConfigurationService;
import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.dao.AccessKeyDAO;
import com.devicehive.dao.AccessKeyPermissionDAO;
import com.devicehive.dao.DeviceDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.*;
import com.devicehive.model.enums.AccessKeyType;
import com.devicehive.model.enums.AccessType;
import com.devicehive.model.oauth.*;
import com.devicehive.model.updates.AccessKeyUpdate;
import com.devicehive.service.helpers.AccessKeyProcessor;
import com.devicehive.service.helpers.OAuthAuthenticationUtils;
import com.devicehive.util.LogExecutionTime;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import javax.ejb.*;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.Response;
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
    private OAuthAuthenticationUtils authenticationUtils;
    @EJB
    private GoogleAuthProvider googleAuthProvider;
    @EJB
    private FacebookAuthProvider facebookAuthProvider;
    @EJB
    private GithubAuthProvider githubAuthProvider;
    @EJB
    private PasswordIdentityProvider passwordIdentityProvider;
    @EJB
    private ConfigurationService configurationService;
    @EJB
    private TimestampService timestampService;

    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;

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
        if (toUpdate.getType() != null) {
            existing.setType(toUpdate.getTypeEnum());
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

    public AccessKey authenticate(@NotNull String key) {
        AccessKey accessKey = accessKeyDAO.get(key);
        if (accessKey == null) {
            return null;
        }
        final Long expirationPeriod = configurationService.getLong(Constants.SESSION_TIMEOUT, Constants.DEFAULT_SESSION_TIMEOUT);
        if (accessKey.getExpirationDate() != null) {
            final Long expiresIn = accessKey.getExpirationDate().getTime() - timestampService.getTimestamp().getTime();
            if (AccessKeyType.SESSION == accessKey.getType() && expiresIn > 0 && expiresIn < expirationPeriod/2) {
                em.refresh(accessKey, LockModeType.PESSIMISTIC_WRITE);
                accessKey.setExpirationDate(new Timestamp(timestampService.getTimestamp().getTime() + expirationPeriod));
                return accessKeyDAO.update(accessKey);
            }
        }
        return accessKey;
    }

    public AccessKey createAccessKey(@NotNull AccessKeyRequest request, IdentityProviderEnum identityProviderEnum) {
        switch (identityProviderEnum) {
            case GOOGLE:
                return googleAuthProvider.createAccessKey(request);
            case FACEBOOK:
                return facebookAuthProvider.createAccessKey(request);
            case GITHUB:
                return githubAuthProvider.createAccessKey(request);
            case PASSWORD: default:
                return passwordIdentityProvider.createAccessKey(request);
        }
    }

    public AccessKey authenticate(@NotNull User user) {
        userService.refreshUserLoginData(user);
        return createExternalAccessToken(user);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    private AccessKey createExternalAccessToken(final User user) {
        AccessKey accessKey = authenticationUtils.prepareAccessKey(user);

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
        newKey.setType(AccessKeyType.OAUTH);
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
    public List<AccessKey> list(Long userId, String label,
                                String labelPattern, Integer type,
                                String sortField, Boolean sortOrderAsc,
                                Integer take, Integer skip) {
        return accessKeyDAO.list(userId, label, labelPattern, type, sortField, sortOrderAsc, take, skip);
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

    @Schedule(dayOfWeek = "Sun", hour = "0", persistent = false)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void removeExpiredKeys() {
        LOGGER.debug("Removing expired access keys");
        accessKeyDAO.deleteOlderThan(timestampService.getTimestamp());
    }
}
