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
import com.devicehive.auth.HivePrincipal;
import com.devicehive.dao.NetworkDao;
import com.devicehive.model.Network;
import com.devicehive.model.User;
import com.devicehive.vo.NetworkVO;
import com.devicehive.vo.NetworkWithUsersAndDevicesVO;
import com.devicehive.vo.UserVO;
import org.springframework.stereotype.Repository;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

@Repository
public class NetworkDaoRdbmsImpl extends RdbmsGenericDao implements NetworkDao {

    @Override
    public List<NetworkVO> findByName(String name) {
        List<Network> result = createNamedQuery(Network.class, "Network.findByName", Optional.of(CacheConfig.get()))
                .setParameter("name", name)
                .getResultList();
        Stream<NetworkVO> objectStream = result.stream().map(Network::convertNetwork);
        return objectStream.collect(Collectors.toList());
    }

    @Override
    public List<NetworkWithUsersAndDevicesVO> getNetworksByIdsAndUsers(Long idForFiltering, Set<Long> networkId, Set<Long> permittedNetworks) {
        TypedQuery<Network> query = createNamedQuery(Network.class, "Network.getNetworksByIdsAndUsers",
                Optional.of(CacheConfig.get()))
                .setParameter("userId", idForFiltering)
                .setParameter("networkIds", networkId)
                .setParameter("permittedNetworks", permittedNetworks);
        List<Network> result = query.getResultList();
        Stream<NetworkWithUsersAndDevicesVO> objectStream = result.stream().map(Network::convertWithDevicesAndUsers);
        return objectStream.collect(Collectors.toList());
    }

    @Override
    public int deleteById(long id) {
        return createNamedQuery("Network.deleteById", Optional.empty())
                .setParameter("id", id)
                .executeUpdate();
    }

    @Override
    public void persist(NetworkVO newNetwork) {
        Network network = Network.convert(newNetwork);
        super.persist(network);
        newNetwork.setId(network.getId());
    }

    @Override
    public NetworkVO find(@NotNull Long networkId) {
        Network network = find(Network.class, networkId);
        return network != null ? Network.convertNetwork(network) : null;
    }

    @Override
    public NetworkVO merge(NetworkVO existing) {
        Network network = find(Network.class, existing.getId());
        network.setName(existing.getName());
        network.setDescription(existing.getDescription());
        network.setEntityVersion(existing.getEntityVersion());
        super.merge(network);
        return existing;
    }

    @Override
    public void assignToNetwork(NetworkVO network, UserVO user) {
        assert network != null && network.getId() != null;
        assert user != null && user.getId() != null;
        Network existing = find(Network.class, network.getId());
        User userReference = reference(User.class, user.getId());
        if (existing.getUsers() == null) {
            existing.setUsers(new HashSet<>());
        }
        existing.getUsers().add(userReference);
        super.merge(existing);
    }

    @Override
    public List<NetworkVO> list(String name, String namePattern, String sortField, boolean sortOrderAsc, Integer take, Integer skip, Optional<HivePrincipal> principal) {
        CriteriaBuilder cb = criteriaBuilder();
        CriteriaQuery<Network> criteria = cb.createQuery(Network.class);
        Root<Network> from = criteria.from(Network.class);

        Predicate[] nameAndPrincipalPredicates = CriteriaHelper.networkListPredicates(cb, from, ofNullable(name), ofNullable(namePattern), principal);
        criteria.where(nameAndPrincipalPredicates);

        CriteriaHelper.order(cb, criteria, from, ofNullable(sortField), sortOrderAsc);

        TypedQuery<Network> query = createQuery(criteria);
        cacheQuery(query, of(CacheConfig.refresh()));
        ofNullable(take).ifPresent(query::setMaxResults);
        ofNullable(skip).ifPresent(query::setFirstResult);
        List<Network> result = query.getResultList();
        Stream<NetworkVO> objectStream = result.stream().map(Network::convertNetwork);
        return objectStream.collect(Collectors.toList());
    }

    @Override
    public Optional<NetworkVO> findFirstByName(String name) {
        return findByName(name).stream().findFirst();
    }

    @Override
    public Optional<NetworkWithUsersAndDevicesVO> findWithUsers(@NotNull long networkId) {
        List<Network> networks = createNamedQuery(Network.class, "Network.findWithUsers", Optional.of(CacheConfig.refresh()))
                .setParameter("id", networkId)
                .getResultList();
        return networks.isEmpty() ? Optional.empty() : Optional.ofNullable(Network.convertWithDevicesAndUsers(networks.get(0)));
    }

    @Override
    public Optional<NetworkVO> findDefaultByUser(long userId) {
        return createNamedQuery(Network.class, "Network.findByUserOrderedById", Optional.of(CacheConfig.refresh()))
                .setParameter("id", userId)
                .getResultList().stream()
                .findFirst()
                .map(Network::convertNetwork);
    }

}
