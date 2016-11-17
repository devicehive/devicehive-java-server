package com.devicehive.service;

/*
 * #%L
 * DeviceHive Java Server Common business logic
 * %%
 * Copyright (C) 2016 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.dao.AccessKeyDao;
import com.devicehive.dao.DeviceDao;
import com.devicehive.exceptions.ActionNotAllowedException;
import com.devicehive.exceptions.IllegalParametersException;
import com.devicehive.model.enums.AccessKeyType;
import com.devicehive.model.enums.AccessType;
import com.devicehive.model.oauth.*;
import com.devicehive.model.rpc.ListAccessKeyRequest;
import com.devicehive.model.rpc.ListAccessKeyResponse;
import com.devicehive.model.updates.AccessKeyUpdate;
import com.devicehive.service.configuration.ConfigurationService;
import com.devicehive.service.helpers.AccessKeyProcessor;
import com.devicehive.service.helpers.OAuthAuthenticationUtils;
import com.devicehive.service.helpers.ResponseConsumer;
import com.devicehive.service.time.TimestampService;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;
import com.devicehive.vo.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

@Deprecated
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
    private DeviceDao deviceDao;
    @Autowired
    private RpcClient rpcClient;


    @Transactional
    public AccessKeyVO create(@NotNull UserVO user, @NotNull AccessKeyVO accessKey) {
        if (accessKey.getLabel() == null) {
            throw new IllegalParametersException(Messages.LABEL_IS_REQUIRED);
        }
        Optional<AccessKeyVO> akOpt = accessKeyDao.getByUserAndLabel(user, accessKey.getLabel());
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
        // TODO should be cascaded
//        for (AccessKeyPermission current : accessKey.getPermissions()) {
//            AccessKeyPermission permission = preparePermission(current);
//            permission.setAccessKey(AccessKey.convert(accessKey));
//            accessKeyPermissionDao.persist(accessKey, permission);
//        }
        return accessKeyDao.find(accessKey.getId());
    }

    @Transactional
    public boolean update(@NotNull Long userId, @NotNull Long keyId, AccessKeyUpdate toUpdate) {
        AccessKeyVO existing = find(keyId, userId);
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
            if (toUpdate.getPermissions().isPresent()) {
                existing.setPermissions(toUpdate.getPermissions().get());
            } else {
                existing.setPermissions(Collections.emptySet());
            }
        }
        accessKeyDao.merge(existing);
        if (toUpdate.getPermissions() != null) {
            if (!toUpdate.getPermissions().isPresent()) {
                logger.error("New permissions shouldn't be empty in request parameters");
                throw new IllegalParametersException(Messages.INVALID_REQUEST_PARAMETERS);
            }
        }

        //TODO [rafa] there are several operations removed, we need to restore it.
//            Set<AccessKeyPermission> permissionsToReplace = toUpdate.getPermissions().get();
//            AccessKeyVO toValidate = toUpdate.convertTo();
//            authenticationUtils.validateActions(toValidate);
//            deleteAccessKeyPermissions(existing);
//            for (AccessKeyPermission current : permissionsToReplace) {
//                AccessKeyPermission permission = preparePermission(current);
//                permission.setAccessKey(AccessKey.convert(existing));
//                accessKeyPermissionDao.persist(existing, permission);
//            }
//        }
        return true;
    }

    @Transactional
    public AccessKeyVO authenticate(@NotNull String key) {
        Optional<AccessKeyVO> accessKeyOpt = accessKeyDao.getByKey(key);

        if (!accessKeyOpt.isPresent()) {
            return null;
        }
        AccessKeyVO accessKey = accessKeyOpt.get();
        final Long expirationPeriod = configurationService.getLong(Constants.SESSION_TIMEOUT, Constants.DEFAULT_SESSION_TIMEOUT);
        if (accessKey.getExpirationDate() != null) {
            final Long expiresIn = accessKey.getExpirationDate().getTime() - timestampService.getTimestamp();
            if (AccessKeyType.SESSION == accessKey.getType() && expiresIn > 0 && expiresIn < expirationPeriod / 2) {
                // previously EntityManager.refresh(accessKey, LockModeType.PESSIMISTIC_WRITE);
                // was used because authenticate from various threads could update expiration date too often and run into deadlock
                // Migration on JWT can solve this problem
                accessKey.setExpirationDate(new Date(timestampService.getTimestamp() + expirationPeriod));
                return accessKeyDao.merge(accessKey);
            }
        }
        return accessKey;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public AccessKeyVO find(@NotNull Long keyId, @NotNull Long userId) {
        return accessKeyDao.getById(keyId, userId);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public boolean hasAccessToNetwork(AccessKeyVO accessKey, NetworkVO targetNetwork) {
        Set<AccessKeyPermissionVO> permissions = accessKey.getPermissions();
        UserVO user = accessKey.getUser();
        boolean hasNullPermission = permissions.stream().anyMatch(perm -> perm.getNetworkIdsAsSet() == null);
        if (hasNullPermission) {
            return userService.hasAccessToNetwork(user, targetNetwork);
        } else {
            Set<Long> allowedNetworks = permissions.stream().map(AccessKeyPermissionVO::getNetworkIdsAsSet)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet());
            UserWithNetworkVO userWithNetworks = userService.findUserWithNetworks(user.getId());
            return allowedNetworks.contains(targetNetwork.getId()) &&
                    (user.isAdmin() || hasNetworksThat(userWithNetworks.getNetworks(), targetNetwork));
        }
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public boolean hasAccessToDevice(AccessKeyVO accessKey, String deviceGuid) {
        Set<AccessKeyPermissionVO> permissions = accessKey.getPermissions();
        Set<String> allowedDevices = new HashSet<>();
        Set<Long> allowedNetworks = new HashSet<>();

        UserWithNetworkVO accessKeyUser = userService.findUserWithNetworks(accessKey.getUser().getId());
        Set<AccessKeyPermissionVO> toRemove = new HashSet<>();

        //TODO [rafa] requires network from device here
        DeviceVO device = deviceDao.findByUUID(deviceGuid);

        for (AccessKeyPermissionVO currentPermission : permissions) {
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
    public AccessKeyVO createAccessKeyFromOAuthGrant(OAuthGrantVO grant, UserVO user, Date now) {
        AccessKeyVO newKey = new AccessKeyVO();
        newKey.setType(AccessKeyType.OAUTH);
        if (grant.getAccessType().equals(AccessType.ONLINE)) {
            Date expirationDate = new Date(now.getTime() + 600000);  //the key is valid for 10 minutes
            newKey.setExpirationDate(expirationDate);
        }
        newKey.setUser(user);
        newKey.setLabel(String.format(Messages.OAUTH_GRANT_TOKEN_LABEL, grant.getClient().getName(), timestampService.getTimestamp()));
        Set<AccessKeyPermissionVO> permissions = new HashSet<>();
        AccessKeyPermissionVO permission = new AccessKeyPermissionVO();
        permission.setDomainArray(grant.getClient().getDomain());
        permission.setActionsArray(StringUtils.split(grant.getScope(), ' '));
        permission.setSubnetsArray(grant.getClient().getSubnet());
        permission.setNetworkIds(grant.getNetworkIds());
        permissions.add(permission);
        newKey.setPermissions(permissions);
        create(user, newKey);
        return newKey;
    }

    @Transactional
    public AccessKeyVO updateAccessKeyFromOAuthGrant(OAuthGrantVO grant, UserVO user, Date now) {
        AccessKeyVO existing = find(grant.getAccessKey().getId(), user.getId());
        if (grant.getAccessType().equals(AccessType.ONLINE)) {
            Date expirationDate = new Date(now.getTime() + 600000);  //the key is valid for 10 minutes
            existing.setExpirationDate(expirationDate);
        } else {
            existing.setExpirationDate(null);
        }
        existing.setLabel(String.format(Messages.OAUTH_GRANT_TOKEN_LABEL, grant.getClient().getName(), timestampService.getTimestamp()));
        Set<AccessKeyPermissionVO> permissions = new HashSet<>();
        AccessKeyPermissionVO permission = new AccessKeyPermissionVO();
        permission.setDomainArray(grant.getClient().getDomain());
        permission.setActionsArray(StringUtils.split(grant.getScope(), ' '));
        permission.setSubnetsArray(grant.getClient().getSubnet());
        permission.setNetworkIds(grant.getNetworkIds());
        permissions.add(permission);
        existing.setPermissions(permissions);
        AccessKeyProcessor keyProcessor = new AccessKeyProcessor();
        String key = keyProcessor.generateKey();
        existing.setKey(key);
        accessKeyDao.merge(existing);
        return existing;
    }

    //@Transactional(propagation = Propagation.NOT_SUPPORTED)
    public CompletableFuture<List<AccessKeyVO>> list(Long userId, String label,
                                                  String labelPattern, Integer type,
                                                  String sortField, Boolean sortOrderAsc,
                                                  Integer take, Integer skip) {
        ListAccessKeyRequest request = new ListAccessKeyRequest();
        request.setUserId(userId);
        request.setLabel(label);
        request.setLabelPattern(labelPattern);
        request.setType(type);
        request.setSortField(sortField);
        request.setSortOrderAsc(sortOrderAsc);
        request.setTake(take);
        request.setSkip(skip);

        CompletableFuture<Response> future = new CompletableFuture<>();

        rpcClient.call(Request.newBuilder()
                .withBody(request).build(), new ResponseConsumer(future));

        return future.thenApply(r -> ((ListAccessKeyResponse) r.getBody()).getAccessKeys());
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public boolean delete(Long userId, @NotNull Long keyId) {
        int removed = ofNullable(userId).map(id -> accessKeyDao.deleteByIdAndUser(keyId, id))
                .orElseGet(() -> accessKeyDao.deleteById(keyId));
        return removed > 0;
    }

    private AccessKeyPermissionVO preparePermission(AccessKeyPermissionVO current) {
        AccessKeyPermissionVO newPermission = new AccessKeyPermissionVO();
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
        int removed = accessKeyDao.deleteOlderThan(timestampService.getDate());
        logger.info("Removed {} expired access keys", removed);
    }

    @Transactional
    public AccessKeyVO getAccessKey(@NotNull String key) {
        Optional<AccessKeyVO> accessKeyOpt = accessKeyDao.getByKey(key);
        AccessKeyVO accessKey = null;
        if (accessKeyOpt.isPresent()) {
            accessKey = accessKeyOpt.get();
        }
        return accessKey;
    }

    private boolean hasPrincipalAccessToDevice(Set<String> allowedDevices, UserVO accessKeyUser, DeviceVO device) {
        boolean hasAccess = false;
        if (allowedDevices.contains(null)) {
            hasAccess = userService.hasAccessToDevice(accessKeyUser, device.getGuid());
        } else if (allowedDevices.contains(device.getGuid())) {
            hasAccess = userService.hasAccessToDevice(accessKeyUser, device.getGuid());
        }
        return hasAccess;
    }

    private boolean hasUserAccessToNetwork(Set<Long> allowedNetworks, UserWithNetworkVO accessKeyUser, DeviceVO device) {
        boolean hasAccess = false;
        boolean testIsAdminOrNetworkListContains = accessKeyUser.isAdmin() || hasNetworksThat(accessKeyUser.getNetworks(), device.getNetwork());
        hasAccess = allowedNetworks.contains(null) ? testIsAdminOrNetworkListContains : (testIsAdminOrNetworkListContains)
                && allowedNetworks.contains(device.getNetwork().getId());
        return hasAccess;
    }

    private boolean hasNetworksThat(Set<NetworkVO> nets, NetworkVO vo) {
        for (NetworkVO net : nets) {
            if (net.getId().equals(vo.getId())) {
                return true;
            }
        }
        return false;
    }
}
