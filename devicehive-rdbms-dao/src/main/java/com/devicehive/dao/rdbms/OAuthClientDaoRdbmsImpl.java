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

import com.devicehive.dao.OAuthClientDao;
import com.devicehive.model.OAuthClient;
import com.devicehive.vo.OAuthClientVO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;

@Profile({"rdbms"})
@Repository
public class OAuthClientDaoRdbmsImpl extends RdbmsGenericDao implements OAuthClientDao {

    @Override
    public int deleteById(Long id) {
        return createNamedQuery("OAuthClient.deleteById", Optional.<CacheConfig>empty())
                .setParameter("id", id)
                .executeUpdate();
    }

    @Override
    public OAuthClientVO getByOAuthId(String oauthId) {
        return OAuthClient.convert(createNamedQuery(OAuthClient.class, "OAuthClient.getByOAuthId", Optional.of(CacheConfig.refresh()))
                .setParameter("oauthId", oauthId)
                .getResultList()
                .stream().findFirst().orElse(null));
    }

    @Override
    public OAuthClientVO getByName(String name) {
        return OAuthClient.convert(createNamedQuery(OAuthClient.class, "OAuthClient.getByName", Optional.of(CacheConfig.refresh()))
                .setParameter("name", name)
                .getResultList()
                .stream().findFirst().orElse(null));
    }

    @Override
    public OAuthClientVO getByOAuthIdAndSecret(String id, String secret) {
        return OAuthClient.convert(createNamedQuery(OAuthClient.class, "OAuthClient.getByOAuthIdAndSecret", Optional.of(CacheConfig.get()))
                .setParameter("oauthId", id)
                .setParameter("secret", secret)
                .getResultList()
                .stream().findFirst().orElse(null));
    }

    @Override
    public OAuthClientVO find(Long id) {
        return OAuthClient.convert(find(OAuthClient.class, id));
    }

    @Override
    public void persist(OAuthClientVO oAuthClient) {
        OAuthClient client = OAuthClient.convert(oAuthClient);
        super.persist(client);
        oAuthClient.setId(client.getId());
    }

    @Override
    public OAuthClientVO merge(OAuthClientVO existing) {
        return OAuthClient.convert(super.merge(OAuthClient.convert(existing)));
    }

    @Override
    public List<OAuthClientVO> get(String name,
                                 String namePattern,
                                 String domain,
                                 String oauthId,
                                 String sortField,
                                 Boolean sortOrderAsc,
                                 Integer take,
                                 Integer skip) {
        CriteriaBuilder cb = criteriaBuilder();
        CriteriaQuery<OAuthClient> cq = cb.createQuery(OAuthClient.class);
        Root<OAuthClient> from = cq.from(OAuthClient.class);

        Predicate[] predicates = CriteriaHelper.oAuthClientListPredicates(cb, from, ofNullable(name), ofNullable(namePattern), ofNullable(domain), ofNullable(oauthId));
        cq.where(predicates);
        CriteriaHelper.order(cb, cq, from, ofNullable(sortField), Boolean.TRUE.equals(sortOrderAsc));

        TypedQuery<OAuthClient> query = createQuery(cq);
        ofNullable(take).ifPresent(query::setMaxResults);
        ofNullable(skip).ifPresent(query::setFirstResult);
        Stream<OAuthClientVO> objectStream = query.getResultList().stream().map(OAuthClient::convert);
        return objectStream.collect(Collectors.toList());
    }
}
