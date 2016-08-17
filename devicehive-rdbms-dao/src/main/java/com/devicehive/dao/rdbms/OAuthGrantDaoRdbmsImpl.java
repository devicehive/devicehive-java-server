package com.devicehive.dao.rdbms;

/*
 * #%L
 * DeviceHive Dao RDBMS Implementation
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

import com.devicehive.dao.OAuthGrantDao;
import com.devicehive.model.OAuthGrant;
import com.devicehive.model.User;
import com.devicehive.vo.OAuthGrantVO;
import com.devicehive.vo.UserVO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.devicehive.dao.rdbms.CriteriaHelper.oAuthGrantsListPredicates;
import static com.devicehive.dao.rdbms.CriteriaHelper.order;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

@Profile({"rdbms"})
@Repository
public class OAuthGrantDaoRdbmsImpl extends RdbmsGenericDao implements OAuthGrantDao {

    @Override
    public OAuthGrantVO getByIdAndUser(UserVO user, Long grantId) {
        OAuthGrant grant = createNamedQuery(OAuthGrant.class, "OAuthGrant.getByIdAndUser",
                of(CacheConfig.refresh()))
                .setParameter("grantId", grantId)
                .setParameter("user", user.getId())
                .getResultList()
                .stream().findFirst().orElse(null);
        return OAuthGrant.convert(grant);
    }

    @Override
    public OAuthGrantVO getById(Long grantId) {
        return OAuthGrant.convert(createNamedQuery(OAuthGrant.class, "OAuthGrant.getById",
                of(CacheConfig.refresh()))
                .setParameter("grantId", grantId)
                .getResultList()
                .stream().findFirst().orElse(null));
    }

    @Override
    public int deleteByUserAndId(UserVO user, Long grantId) {
        return createNamedQuery("OAuthGrant.deleteByUserAndId", of(CacheConfig.refresh()))
                .setParameter("grantId", grantId)
                .setParameter("user", user.getId())
                .executeUpdate();
    }

    @Override
    public OAuthGrantVO getByCodeAndOAuthID(String authCode, String clientOAuthID) {
        return OAuthGrant.convert(createNamedQuery(OAuthGrant.class, "OAuthGrant.getByCodeAndOAuthID", of(CacheConfig.refresh()))
                .setParameter("authCode", authCode)
                .setParameter("oauthId", clientOAuthID)
                .getResultList()
                .stream().findFirst().orElse(null));
    }

    @Override
    public OAuthGrantVO find(Long id) {
        return OAuthGrant.convert(find(OAuthGrant.class, id));
    }

    @Override
    public void persist(OAuthGrantVO oAuthGrant) {
        OAuthGrant grant = OAuthGrant.convert(oAuthGrant);
        super.persist(grant);
        oAuthGrant.setId(grant.getId());
    }

    @Override
    public OAuthGrantVO merge(OAuthGrantVO existing) {
        return OAuthGrant.convert(super.merge(OAuthGrant.convert(existing)));
    }

    @Override
    public List<OAuthGrantVO> list(@NotNull UserVO user,
                                 Date start,
                                 Date end,
                                 String clientOAuthId,
                                 Integer type,
                                 String scope,
                                 String redirectUri,
                                 Integer accessType,
                                 String sortField,
                                 Boolean sortOrder,
                                 Integer take,
                                 Integer skip) {
        CriteriaBuilder cb = criteriaBuilder();
        CriteriaQuery<OAuthGrant> cq = cb.createQuery(OAuthGrant.class);
        Root<OAuthGrant> from = cq.from(OAuthGrant.class);
        from.fetch("accessKey", JoinType.LEFT).fetch("permissions");
        from.fetch("client");

        Predicate[] predicates = oAuthGrantsListPredicates(cb, from, user, ofNullable(start), ofNullable(end), ofNullable(clientOAuthId), ofNullable(type), ofNullable(scope),
                ofNullable(redirectUri), ofNullable(accessType));
        cq.where(predicates);
        order(cb, cq, from, ofNullable(sortField), Boolean.TRUE.equals(sortOrder));

        TypedQuery<OAuthGrant> query = createQuery(cq);
        ofNullable(take).ifPresent(query::setMaxResults);
        ofNullable(skip).ifPresent(query::setFirstResult);

        return query.getResultList().stream().map(OAuthGrant::convert).collect(Collectors.toList());
    }
}
