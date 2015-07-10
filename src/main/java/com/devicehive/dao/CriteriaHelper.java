package com.devicehive.dao;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.dao.filter.AccessKeyBasedFilterForDevices;
import com.devicehive.model.*;

import javax.persistence.criteria.*;
import java.util.*;

import static java.util.Optional.ofNullable;

public class CriteriaHelper {

    /**
     * Creates an array of JPA predicates for networks list query. Add filter predicates for name and principal if required.
     * 1) if name is specified adds 'name = ?' predicate
     * 2) if name pattern is specified adds 'name like ?' predicate
     * 3) if principal is user of key without ADMIN role adds predicate for filtering not assigned networks
     * 4) if principal is key which has permissions only to specific networks adds 'network.id in (allowed_networks)' predicates
     *
     * @return array of above predicates
     * @see {@link com.devicehive.service.NetworkService#list(String, String, String, boolean, Integer, Integer, HivePrincipal)}
     */
    public static Predicate[] networkListPredicates(CriteriaBuilder cb, Root<Network> from, Optional<String> nameOpt, Optional<String> namePatternOpt, Optional<HivePrincipal> principalOpt) {
        List<Predicate> predicates = new LinkedList<>();

        nameOpt.ifPresent(name ->
                predicates.add(cb.equal(from.get("name"), name)));

        namePatternOpt.ifPresent(pattern ->
                predicates.add(cb.like(from.get("name"), pattern)));

        principalOpt.flatMap(principal -> {
            User user = principal.getUser();
            if (user == null && principal.getKey() != null) {
                user = principal.getKey().getUser();
            }
            return ofNullable(user);
        }).ifPresent(user -> {
            if (!user.isAdmin()) {
                predicates.add(from.join("users").in(user));
            }
        });

        principalOpt.map(HivePrincipal::getKey).ifPresent(key ->
                        predicates.add(cb.or(networkPermissionsPredicates(cb, from, key.getPermissions())))
        );

        return predicates.toArray(new Predicate[predicates.size()]);
    }

    public static Predicate[] networkPermissionsPredicates(CriteriaBuilder cb, Root<?> from, Set<AccessKeyPermission> permissions) {
        List<Predicate> predicates = new ArrayList<>();
        for (AccessKeyBasedFilterForDevices extraFilter : AccessKeyBasedFilterForDevices.createExtraFilters(permissions)) {
            List<Predicate> filter = new ArrayList<>();
            if (extraFilter.getNetworkIds() != null) {
                filter.add(from.get("id").in(extraFilter.getNetworkIds()));
            }
            predicates.add(cb.and(filter.toArray(new Predicate[filter.size()])));
        }
        return predicates.toArray(new Predicate[predicates.size()]);
    }

    /**
     * Adds ORDER BY ... ASC/DESC to query
     * Mutates provided criteria query
     * @param sortFieldOpt - field to sort by (field name in JPA Entity class)
     * @param asc - true if order should be ASC, false otherwise
     */
    public static void order(CriteriaBuilder cb, CriteriaQuery<?> cq, Root<?> from, Optional<String> sortFieldOpt, boolean asc) {
        sortFieldOpt.ifPresent(sortField -> {
            Order order = asc ? cb.asc(from.get(sortField)) : cb.desc(from.get(sortField));
            cq.orderBy(order);
        });
    }

    public static Predicate[] userListPredicates(CriteriaBuilder cb, Root<User> from, Optional<String> loginOpt, Optional<String> loginPattern, Optional<Integer> roleOpt, Optional<Integer> statusOpt) {
        List<Predicate> predicates = new LinkedList<>();

        if (loginPattern.isPresent()) {
            loginPattern.ifPresent(pattern ->
                    predicates.add(cb.like(from.get("login"), pattern)));
        } else {
            loginOpt.ifPresent(login ->
                    predicates.add(cb.equal(from.get("login"), login)));
        }

        roleOpt.ifPresent(role -> predicates.add(cb.equal(from.get("role"), role)));
        statusOpt.ifPresent(status -> predicates.add(cb.equal(from.get("status"), status)));

        return predicates.toArray(new Predicate[predicates.size()]);
    }

    public static Predicate[] accessKeyListPredicates(CriteriaBuilder cb, Root<AccessKey> from, Long userId, Optional<String> labelOpt, Optional<String> labelPatten,
                                                      Optional<Integer> typeOpt) {
        List<Predicate> predicates = new LinkedList<>();

        Join user = (Join) from.fetch("user", JoinType.LEFT);
        predicates.add(cb.equal(user.get("id"), userId));

        if (labelPatten.isPresent()) {
            labelPatten.ifPresent(pattern -> predicates.add(cb.like(from.get("label"), pattern)));
        } else {
            labelOpt.ifPresent(label -> predicates.add(cb.equal(from.get("label"), label)));
        }

        typeOpt.ifPresent(type -> predicates.add(cb.equal(from.get("type"), type)));

        return predicates.toArray(new Predicate[predicates.size()]);
    }

    public static Predicate[] oAuthGrantsListPredicates(CriteriaBuilder cb, Root<OAuthGrant> from, User user, Optional<Date> startOpt, Optional<Date> endOpt, Optional<String> oAuthIdOpt,
                                                        Optional<Integer> typeOpt, Optional<String> scopeOpt, Optional<String> redirectUri, Optional<Integer> accessType) {
        List<Predicate> predicates = new LinkedList<>();

        if (!user.isAdmin()) {
            predicates.add(from.join("user").in(user));
        }

        startOpt.ifPresent(start -> predicates.add(cb.greaterThan(from.get("timestamp"), start)));
        endOpt.ifPresent(end -> predicates.add(cb.lessThan(from.get("timestamp"), end)));
        oAuthIdOpt.ifPresent(id -> predicates.add(cb.equal(from.join("client").get("oauthId"), id)));
        typeOpt.ifPresent(type -> predicates.add(cb.equal(from.get("type"), type)));
        scopeOpt.ifPresent(scope -> predicates.add(cb.equal(from.get("scope"), scope)));
        redirectUri.ifPresent(uri -> predicates.add(cb.equal(from.get("redirectUri"), uri)));
        accessType.ifPresent(at -> predicates.add(cb.equal(from.get("accessType"), at)));

        return predicates.toArray(new Predicate[predicates.size()]);
    }

}
