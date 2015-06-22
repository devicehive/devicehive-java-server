package com.devicehive.service;

import com.devicehive.auth.AccessKeyAction;
import com.devicehive.auth.CheckPermissionsHelper;
import com.devicehive.auth.HiveAuthentication;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.ConfigurationService;
import com.devicehive.configuration.Messages;
import com.devicehive.dao.CacheConfig;
import com.devicehive.dao.CriteriaHelper;
import com.devicehive.dao.GenericDAO;
import com.devicehive.dao.NetworkDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.*;
import com.devicehive.model.updates.NetworkUpdate;
import com.devicehive.util.HiveValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Collections.singletonList;
import static java.util.Optional.*;
import static javax.ws.rs.core.Response.Status.*;

@Component
public class NetworkService {
    private static final Logger logger = LoggerFactory.getLogger(NetworkService.class);

    public static final String ALLOW_NETWORK_AUTO_CREATE = "allowNetworkAutoCreate";

    @Autowired
    private UserService userService;
    @Autowired
    private AccessKeyService accessKeyService;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private HiveValidator hiveValidator;
    @Autowired
    private GenericDAO genericDAO;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Network getWithDevicesAndDeviceClasses(@NotNull Long networkId, @NotNull HiveAuthentication hiveAuthentication) {
        HiveAuthentication.HiveAuthDetails details = (HiveAuthentication.HiveAuthDetails) hiveAuthentication.getDetails();
        HivePrincipal principal = (HivePrincipal) hiveAuthentication.getPrincipal();

        Optional<Network> result = of(principal)
                .flatMap(pr -> {
                    if (pr.getUser() != null)
                        return of(pr.getUser());
                    else if (pr.getKey() != null && pr.getKey().getUser() != null)
                        return of(pr.getKey().getUser());
                    else
                        return empty();
                }).flatMap(user -> {
                    Long idForFiltering = user.isAdmin() ? null : user.getId();
                    TypedQuery<Network> query = genericDAO.createNamedQuery(Network.class, Network.Queries.Names.FILTER_BY_ID_AND_USER, of(CacheConfig.bypass()))
                            .setParameter("userId", idForFiltering)
                            .setParameter("networkIds", singletonList(networkId));
                    List<Network> found = query.getResultList();
                    return found.stream().findFirst();
                }).map(network -> {
                    if (principal.getKey() != null) {
                        Set<AccessKeyPermission> permissions = principal.getKey().getPermissions();
                        Set<AccessKeyPermission> filtered = CheckPermissionsHelper
                                .filterPermissions(permissions, AccessKeyAction.GET_DEVICE,
                                        details.getClientInetAddress(), details.getOrigin());
                        if (filtered.isEmpty()) {
                            network.setDevices(Collections.emptySet());
                        }
                    }
                    return network;
                });

        return result.orElse(null);
    }

    @Transactional
    public boolean delete(long id) {
        logger.trace("About to execute named query {} for ", Network.Queries.Names.DELETE_BY_ID);
        int result = genericDAO.createNamedQuery(Network.Queries.Names.DELETE_BY_ID, Optional.<CacheConfig>empty())
                .setParameter(Network.Queries.Parameters.ID, id)
                .executeUpdate();
        logger.debug("Deleted {} rows from Network table", result);
        return result > 0;
    }

    @Transactional
    public Network create(Network newNetwork) {
        logger.debug("Creating network {}", newNetwork);
        if (newNetwork.getId() != null) {
            logger.error("Can't create network entity with id={} specified", newNetwork.getId());
            throw new HiveException(Messages.ID_NOT_ALLOWED, BAD_REQUEST.getStatusCode());
        }
        List<Network> existing = genericDAO.createNamedQuery(Network.class, Network.Queries.Names.FIND_BY_NAME, Optional.of(CacheConfig.get()))
                .setParameter(Network.Queries.Parameters.NAME, newNetwork.getName())
                .getResultList();
        if (!existing.isEmpty()) {
            logger.error("Network with name {} already exists", newNetwork.getName());
            throw new HiveException(Messages.DUPLICATE_NETWORK, FORBIDDEN.getStatusCode());
        }
        genericDAO.persist(newNetwork);
        logger.info("Entity {} created successfully", newNetwork);
        return newNetwork;
    }

    @Transactional
    public Network update(@NotNull Long networkId, NetworkUpdate networkUpdate) {
        Network existing = genericDAO.find(Network.class, networkId);
        if (existing == null) {
            throw new HiveException(String.format(Messages.NETWORK_NOT_FOUND, networkId), NOT_FOUND.getStatusCode());
        }
        if (networkUpdate.getKey() != null) {
            existing.setKey(networkUpdate.getKey().getValue());
        }
        if (networkUpdate.getName() != null) {
            existing.setName(networkUpdate.getName().getValue());
        }
        if (networkUpdate.getDescription() != null) {
            existing.setDescription(networkUpdate.getDescription().getValue());
        }
        hiveValidator.validate(existing);
        return genericDAO.merge(existing);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<Network> list(String name,
                              String namePattern,
                              String sortField,
                              boolean sortOrderAsc,
                              Integer take,
                              Integer skip,
                              HivePrincipal principal) {
        Optional<HivePrincipal> principalOpt = ofNullable(principal);
        principalOpt.map(HivePrincipal::getDevice).ifPresent(device -> {
            throw new HiveException("Can not get access to networks", 403);
        });

        CriteriaBuilder cb = genericDAO.criteriaBuilder();
        CriteriaQuery<Network> criteria = cb.createQuery(Network.class);
        Root<Network> from = criteria.from(Network.class);

        Predicate[] nameAndPrincipalPredicates = CriteriaHelper.networkListPredicates(cb, from, ofNullable(name), ofNullable(namePattern), principalOpt);
        criteria.where(nameAndPrincipalPredicates);

        CriteriaHelper.order(cb, criteria, from, ofNullable(sortField), sortOrderAsc);

        TypedQuery<Network> query = genericDAO.createQuery(criteria);
        genericDAO.cacheQuery(query, of(CacheConfig.refresh()));
        ofNullable(take).ifPresent(query::setMaxResults);
        ofNullable(skip).ifPresent(query::setFirstResult);
        return query.getResultList();
    }

    @Transactional
    public Network createOrVerifyNetwork(NullableWrapper<Network> networkNullable) {
        return createOrVerifyNetwork(networkNullable, network -> null, () -> false);
    }

    private Network createOrVerifyNetwork(NullableWrapper<Network> networkNullable, Function<Network, Object> onUpdate, Supplier<Boolean> onCreate) {
        //case network is not defined
        if (networkNullable == null || networkNullable.getValue() == null) {
            return null;
        }

        Network network = networkNullable.getValue();

        Optional<Network> storedOpt = ofNullable(network.getId())
                .map(id -> ofNullable(genericDAO.find(Network.class, id)))
                .orElseGet(() ->
                        genericDAO.createNamedQuery(Network.class, Network.Queries.Names.FIND_BY_NAME, empty())
                                .setParameter(Network.Queries.Parameters.NAME, network.getName())
                                .getResultList()
                                .stream().findFirst());
        if (storedOpt.isPresent()) {
            Network stored = storedOpt.get();
            if (stored.getKey() != null && !stored.getKey().equals(network.getKey())) {
                throw new HiveException(Messages.INVALID_NETWORK_KEY, FORBIDDEN.getStatusCode());
            }
            onUpdate.apply(stored);
            return stored;
        } else {
            if (network.getId() != null) {
                throw new HiveException(Messages.INVALID_REQUEST_PARAMETERS, BAD_REQUEST.getStatusCode());
            }
            boolean allowed = configurationService.getBoolean(ALLOW_NETWORK_AUTO_CREATE, false);
            if (!onCreate.get() && !allowed) {
                throw new HiveException(Messages.NETWORK_CREATION_NOT_ALLOWED, FORBIDDEN.getStatusCode());
            }
            genericDAO.persist(network);
            return network;
        }

    }

    @Transactional
    public Network createOrUpdateNetworkByUser(NullableWrapper<Network> network, User user) {
        return createOrVerifyNetwork(network, stored -> {
            if (!userService.hasAccessToNetwork(user, stored)) {
                throw new HiveException(Messages.NO_ACCESS_TO_NETWORK, FORBIDDEN.getStatusCode());
            }
            return null;
        }, user::isAdmin);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Network createOrVerifyNetworkByKey(NullableWrapper<Network> network, AccessKey key) {
        return createOrVerifyNetwork(network, stored -> {
            if (stored.getKey() != null && !accessKeyService.hasAccessToNetwork(key, stored)) {
                throw new HiveException(Messages.NO_ACCESS_TO_NETWORK, FORBIDDEN.getStatusCode());
            }
            return null;
        }, () -> false);
    }

}
