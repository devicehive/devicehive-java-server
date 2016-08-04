package com.devicehive.service;

import com.devicehive.configuration.ConfigurationService;
import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.dao.*;
import com.devicehive.exceptions.ActionNotAllowedException;
import com.devicehive.exceptions.IllegalParametersException;
import com.devicehive.model.*;
import com.devicehive.model.enums.AccessKeyType;
import com.devicehive.model.enums.AccessType;
import com.devicehive.model.oauth.*;
import com.devicehive.model.updates.AccessKeyUpdate;
import com.devicehive.service.helpers.AccessKeyProcessor;
import com.devicehive.service.helpers.OAuthAuthenticationUtils;
import com.devicehive.service.time.TimestampService;
import com.devicehive.vo.AccessKeyRequestVO;
import com.devicehive.vo.DeviceVO;
import com.devicehive.vo.NetworkVO;
import com.devicehive.vo.OAuthGrantVO;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

@Component
public class AccessKeyService {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(AccessKeyService.class);

    @Autowired
    private UserService userService;
    @Autowired
    private OAuthAuthenticationUtils authenticationUtils;
    @Autowired
    private GoogleAuthProvider googleAuthProvider;
    @Autowired
    private FacebookAuthProvider facebookAuthProvider;
    @Autowired
    private GithubAuthProvider githubAuthProvider;
    @Autowired
    private PasswordIdentityProvider passwordIdentityProvider;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private TimestampService timestampService;

    @Autowired
    private AccessKeyDao accessKeyDao;

    @Autowired
    private AccessKeyPermissionDao accessKeyPermissionDao;

    @Autowired
    private DeviceDao deviceDao;


    @Transactional
    public AccessKey create(@NotNull User user, @NotNull AccessKey accessKey) {
        if (accessKey.getLabel() == null) {
            throw new IllegalParametersException(Messages.LABEL_IS_REQUIRED);
        }
        Optional<AccessKey> akOpt = accessKeyDao.getByUserAndLabel(user, accessKey.getLabel());
        if (akOpt.isPresent()) {
            logger.error("Access key with label {} already exists", accessKey.getLabel());
            throw new ActionNotAllowedException(Messages.DUPLICATE_LABEL_FOUND);
        }
        if (accessKey.getId() != null) {
            logger.error("Access key id shouldn't be present in request parameters");
            throw new IllegalParametersException(Messages.INVALID_REQUEST_PARAMETERS);
        }
        authenticationUtils.validateActions(accessKey);
        AccessKeyProcessor keyProcessor = new AccessKeyProcessor();
        String key = keyProcessor.generateKey();
        accessKey.setKey(key);
        accessKey.setUser(user);
        accessKeyDao.persist(accessKey);
        for (AccessKeyPermission current : accessKey.getPermissions()) {
            AccessKeyPermission permission = preparePermission(current);
            permission.setAccessKey(accessKey);
            accessKeyPermissionDao.persist(accessKey, permission);
        }
        return accessKeyDao.find(accessKey.getId());
    }

    @Transactional
    public boolean update(@NotNull Long userId, @NotNull Long keyId, AccessKeyUpdate toUpdate) {
        AccessKey existing = find(keyId, userId);
        if (existing == null) {
            return false;
        }
        if (toUpdate == null) {
            return true;
        }

        if (toUpdate.getLabel() != null) {
            existing.setLabel(toUpdate.getLabel().orElse(null));
        }
        if (toUpdate.getExpirationDate() != null) {
            existing.setExpirationDate(toUpdate.getExpirationDate().orElse(null));
        }
        if (toUpdate.getType() != null) {
            existing.setType(toUpdate.getType().map(v -> toUpdate.getTypeEnum()).orElse(null));
        }
        if (toUpdate.getPermissions() != null) {
            if (!toUpdate.getPermissions().isPresent()) {
                logger.error("New permissions shouldn't be empty in request parameters");
                throw new IllegalParametersException(Messages.INVALID_REQUEST_PARAMETERS);
            }

            Set<AccessKeyPermission> permissionsToReplace = toUpdate.getPermissions().get();
            AccessKey toValidate = toUpdate.convertTo();
            authenticationUtils.validateActions(toValidate);
            deleteAccessKeyPermissions(existing);
            for (AccessKeyPermission current : permissionsToReplace) {
                AccessKeyPermission permission = preparePermission(current);
                permission.setAccessKey(existing);
                accessKeyPermissionDao.persist(existing, permission);
            }
        }
        return true;
    }

    @Transactional
    public AccessKey authenticate(@NotNull String key) {
        Optional<AccessKey> accessKeyOpt = accessKeyDao.getByKey(key);
        if (!accessKeyOpt.isPresent()) {
            return null;
        }
        AccessKey accessKey = accessKeyOpt.get();
        final Long expirationPeriod = configurationService.getLong(Constants.SESSION_TIMEOUT, Constants.DEFAULT_SESSION_TIMEOUT);
        if (accessKey.getExpirationDate() != null) {
            final Long expiresIn = accessKey.getExpirationDate().getTime() - timestampService.getTimestamp().getTime();
            if (AccessKeyType.SESSION == accessKey.getType() && expiresIn > 0 && expiresIn < expirationPeriod / 2) {
                // previously EntityManager.refresh(accessKey, LockModeType.PESSIMISTIC_WRITE);
                // was used because authenticate from various threads could update expiration date too often and run into deadlock
                // Migration on JWT can solve this problem
                accessKey.setExpirationDate(new Date(timestampService.getTimestamp().getTime() + expirationPeriod));
                return accessKeyDao.merge(accessKey);
            }
        }
        return accessKey;
    }

    public AccessKey createAccessKey(@NotNull AccessKeyRequestVO request, IdentityProviderEnum identityProviderEnum) {
        switch (identityProviderEnum) {
            case GOOGLE:
                return googleAuthProvider.createAccessKey(request);
            case FACEBOOK:
                return facebookAuthProvider.createAccessKey(request);
            case GITHUB:
                return githubAuthProvider.createAccessKey(request);
            case PASSWORD:
            default:
                return passwordIdentityProvider.createAccessKey(request);
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public AccessKey authenticate(@NotNull User user) {
        userService.refreshUserLoginData(user);

        AccessKey accessKey = authenticationUtils.prepareAccessKey(user);

        Set<AccessKeyPermission> permissions = new HashSet<>();
        final AccessKeyPermission permission = authenticationUtils.preparePermission(user.getRole());
        permissions.add(permission);
        accessKey.setPermissions(permissions);
        accessKeyDao.persist(accessKey);

        permission.setAccessKey(accessKey);
        accessKeyPermissionDao.persist(accessKey, permission);
        return accessKey;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public AccessKey find(@NotNull Long keyId, @NotNull Long userId) {
        return accessKeyDao.getById(keyId, userId);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public boolean hasAccessToNetwork(AccessKey accessKey, NetworkVO targetNetwork) {
        Set<AccessKeyPermission> permissions = accessKey.getPermissions();
        User user = accessKey.getUser();
        boolean hasNullPermission = permissions.stream().anyMatch(perm -> perm.getNetworkIdsAsSet() == null);
        if (hasNullPermission) {
            return userService.hasAccessToNetwork(user, targetNetwork);
        } else {
            Set<Long> allowedNetworks = permissions.stream().map(AccessKeyPermission::getNetworkIdsAsSet)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet());
            user = userService.findUserWithNetworks(user.getId());
            Network nw = Network.convert(targetNetwork);
            return allowedNetworks.contains(targetNetwork.getId()) &&
                    (user.isAdmin() || user.getNetworks().contains(nw));
        }
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public boolean hasAccessToDevice(AccessKey accessKey, String deviceGuid) {
        Set<AccessKeyPermission> permissions = accessKey.getPermissions();
        Set<String> allowedDevices = new HashSet<>();
        Set<Long> allowedNetworks = new HashSet<>();

        User accessKeyUser = userService.findUserWithNetworks(accessKey.getUser().getId());
        Set<AccessKeyPermission> toRemove = new HashSet<>();

        //TODO [rafa] requires network from device here
        DeviceVO device = deviceDao.findByUUID(deviceGuid);

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
        boolean hasAccess = hasPrincipalAccessToDevice(allowedDevices, accessKeyUser, device);

        if (hasAccess) {
            hasAccess = hasUserAccessToNetwork(allowedNetworks, accessKeyUser, device);
        }

        return hasAccess;
    }

    @Transactional
    public AccessKey createAccessKeyFromOAuthGrant(OAuthGrantVO grant, User user, Date now) {
        AccessKey newKey = new AccessKey();
        newKey.setType(AccessKeyType.OAUTH);
        if (grant.getAccessType().equals(AccessType.ONLINE)) {
            Date expirationDate = new Date(now.getTime() + 600000);  //the key is valid for 10 minutes
            newKey.setExpirationDate(expirationDate);
        }
        newKey.setUser(user);
        newKey.setLabel(String.format(Messages.OAUTH_GRANT_TOKEN_LABEL, grant.getClient().getName(), System.currentTimeMillis()));
        Set<AccessKeyPermission> permissions = new HashSet<>();
        AccessKeyPermission permission = new AccessKeyPermission();
        permission.setDomainArray(grant.getClient().getDomain());
        permission.setActionsArray(StringUtils.split(grant.getScope(), ' '));
        permission.setSubnetsArray(grant.getClient().getSubnet());
        permission.setNetworkIds(grant.getNetworkIds());
        permissions.add(permission);
        newKey.setPermissions(permissions);
        create(user, newKey);
        return newKey;
    }

    private void deleteAccessKeyPermissions(AccessKey key) {
        logger.debug("Deleting all permission of access key {}", key.getId());
        int deleted = accessKeyPermissionDao.deleteByAccessKey(key);
        logger.info("Deleted {} permissions by access key {}", deleted, key.getId());
    }

    @Transactional
    public AccessKey updateAccessKeyFromOAuthGrant(OAuthGrantVO grant, User user, Date now) {
        AccessKey existing = find(grant.getAccessKey().getId(), user.getId());
        deleteAccessKeyPermissions(existing);
        if (grant.getAccessType().equals(AccessType.ONLINE)) {
            Date expirationDate = new Date(now.getTime() + 600000);  //the key is valid for 10 minutes
            existing.setExpirationDate(expirationDate);
        } else {
            existing.setExpirationDate(null);
        }
        existing.setLabel(String.format(Messages.OAUTH_GRANT_TOKEN_LABEL, grant.getClient().getName(), System.currentTimeMillis()));
        Set<AccessKeyPermission> permissions = new HashSet<>();
        AccessKeyPermission permission = new AccessKeyPermission();
        permission.setDomainArray(grant.getClient().getDomain());
        permission.setActionsArray(StringUtils.split(grant.getScope(), ' '));
        permission.setSubnetsArray(grant.getClient().getSubnet());
        permission.setNetworkIds(grant.getNetworkIds());
        permissions.add(permission);
        existing.setPermissions(permissions);
        AccessKeyProcessor keyProcessor = new AccessKeyProcessor();
        String key = keyProcessor.generateKey();
        existing.setKey(key);
        for (AccessKeyPermission current : permissions) {
            current.setAccessKey(existing);
            accessKeyPermissionDao.persist(existing, current);
        }
        accessKeyDao.merge(existing);
        return existing;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<AccessKey> list(Long userId, String label,
                                String labelPattern, Integer type,
                                String sortField, Boolean sortOrderAsc,
                                Integer take, Integer skip) {
        return accessKeyDao.list(userId, label,
                labelPattern, type,
                sortField, sortOrderAsc,
                take, skip);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public boolean delete(Long userId, @NotNull Long keyId) {
        int removed = ofNullable(userId).map(id -> accessKeyDao.deleteByIdAndUser(keyId, id))
                .orElseGet(() -> accessKeyDao.deleteById(keyId));
        return removed > 0;
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

    @Scheduled(cron = "0 0 0 * * SUN")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void removeExpiredKeys() {
        logger.debug("Removing expired access keys");
        int removed = accessKeyDao.deleteOlderThan(timestampService.getTimestamp());
        logger.info("Removed {} expired access keys", removed);
    }

    private boolean hasPrincipalAccessToDevice(Set<String> allowedDevices, User accessKeyUser, DeviceVO device) {
        boolean hasAccess = false;
        if (allowedDevices.contains(null)) {
            hasAccess = userService.hasAccessToDevice(accessKeyUser, device.getGuid());
        } else if (allowedDevices.contains(device.getGuid())) {
            hasAccess = userService.hasAccessToDevice(accessKeyUser, device.getGuid());
        }
        return hasAccess;
    }

    private boolean hasUserAccessToNetwork(Set<Long> allowedNetworks, User accessKeyUser, DeviceVO device) {
        boolean hasAccess = false;
        boolean testIsAdminOrNetworkListContains = accessKeyUser.isAdmin() || hasNetworksThat(accessKeyUser.getNetworks(), device.getNetwork());
        hasAccess = allowedNetworks.contains(null) ? testIsAdminOrNetworkListContains : (testIsAdminOrNetworkListContains)
                && allowedNetworks.contains(device.getNetwork().getId());
        return hasAccess;
    }

    private boolean hasNetworksThat(Set<Network> nets, NetworkVO vo) {
        for (Network net : nets) {
            if (net.getId().equals(vo.getId())) {
                return true;
            }
        }
        return false;
    }
}
