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

import com.devicehive.dao.UserDao;
import com.devicehive.model.DeviceType;
import com.devicehive.model.Network;
import com.devicehive.model.User;
import com.devicehive.vo.*;
import org.springframework.stereotype.Repository;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

@Repository
public class UserDaoRdbmsImpl extends RdbmsGenericDao implements UserDao {

    @Override
    public Optional<UserVO> findByName(String name) {
        Optional<User> login = createNamedQuery(User.class, "User.findByName", of(CacheConfig.get()))
                .setParameter("login", name)
                .getResultList()
                .stream().findFirst();
        return optionalUserConvertToVo(login);
    }

    @Override
    public long hasAccessToNetwork(UserVO user, NetworkVO network) {
        Network nw = reference(Network.class, network.getId());
        return createNamedQuery(Long.class, "User.hasAccessToNetwork", empty())
                .setParameter("user", user.getId())
                .setParameter("network", nw)
                .getSingleResult();
    }

    @Override
    public long hasAccessToDevice(UserVO user, String deviceId) {
        return createNamedQuery(Long.class, "User.hasAccessToDevice", empty())
                .setParameter("user", user.getId())
                .setParameter("deviceId", deviceId)
                .getSingleResult();
    }

    @Override
    public UserWithNetworkVO getWithNetworksById(long id) {
        User user = createNamedQuery(User.class, "User.getWithNetworksById", of(CacheConfig.get()))
                .setParameter("id", id)
                .getResultList()
                .stream().findFirst().orElse(null);
        if (user == null) {
            return null;
        }
        UserVO vo = User.convertToVo(user);
        UserWithNetworkVO userWithNetworkVO = UserWithNetworkVO.fromUserVO(vo);
        //TODO [rafa] change here to bulk fetch data
        if (user.getNetworks() != null) {
            for (Network network : user.getNetworks()) {
                NetworkVO networkVo = Network.convertNetwork(network);
                userWithNetworkVO.getNetworks().add(networkVo);
            }
        }
        return userWithNetworkVO;
    }

    @Override
    public UserWithDeviceTypeVO getWithDeviceTypeById(long id) {
        User user = createNamedQuery(User.class, "User.getWithDeviceTypesById", of(CacheConfig.get()))
                .setParameter("id", id)
                .getResultList()
                .stream().findFirst().orElse(null);
        if (user == null) {
            return null;
        }
        UserVO vo = User.convertToVo(user);
        UserWithDeviceTypeVO userWithDeviceTypeVO = UserWithDeviceTypeVO.fromUserVO(vo);
        //TODO [rafa] change here to bulk fetch data
        if (user.getDeviceTypes() != null) {
            for (DeviceType deviceType : user.getDeviceTypes()) {
                DeviceTypeVO deviceTypeVO = DeviceType.convertDeviceType(deviceType);
                userWithDeviceTypeVO.getDeviceTypes().add(deviceTypeVO);
            }
        }
        return userWithDeviceTypeVO;
    }

    @Override
    public int deleteById(long id) {
        return createNamedQuery("User.deleteById", of(CacheConfig.bypass()))
                .setParameter("id", id)
                .executeUpdate();
    }

    @Override
    public UserVO find(Long id) {
        User user = find(User.class, id);
        return User.convertToVo(user);
    }

    @Override
    public void persist(UserVO user) {
        User entity = User.convertToEntity(user);
        super.persist(entity);
        user.setId(entity.getId());
    }

    @Override
    public UserVO merge(UserVO existing) {
        User entity = User.convertToEntity(existing);
        User merge = super.merge(entity);
        return User.convertToVo(merge);
    }

    @Override
    public void unassignNetwork(@NotNull UserVO existingUser, @NotNull long networkId) {
        createNamedQuery(Network.class, "Network.findWithUsers", of(CacheConfig.refresh()))
                .setParameter("id", networkId)
                .getResultList()
                .stream().findFirst()
                .ifPresent(existingNetwork -> {
                    User usr = new User();
                    usr.setId(existingUser.getId());
                    existingNetwork.getUsers().remove(usr);
                    merge(existingNetwork);
                });
    }

    @Override
    public void unassignDeviceType(@NotNull UserVO existingUser, @NotNull long deviceTypeId) {
        createNamedQuery(DeviceType.class, "DeviceType.findWithUsers", of(CacheConfig.refresh()))
                .setParameter("id", deviceTypeId)
                .getResultList()
                .stream().findFirst()
                .ifPresent(existingDeviceType -> {
                    User usr = new User();
                    usr.setId(existingUser.getId());
                    existingDeviceType.getUsers().remove(usr);
                    merge(existingDeviceType);
                });
    }

    @Override
    public UserVO allowAllDeviceTypes(UserWithDeviceTypeVO existingUser) {
        User entity = User.convertToEntity(existingUser);
        entity.setAllDeviceTypesAvailable(true);
        //TODO - add single named query to avoid cycling through all device types
        for (DeviceType dt: entity.getDeviceTypes()) {
            unassignDeviceType(existingUser, dt.getId());
        }
        entity.setDeviceTypes(Collections.EMPTY_SET);
        User merge = super.merge(entity);
        return User.convertToVo(merge);
    }

    @Override
    public UserVO disallowAllDeviceTypes(UserVO existingUser) {
        User entity = User.convertToEntity(existingUser);
        entity.setAllDeviceTypesAvailable(false);
        User merge = super.merge(entity);
        return User.convertToVo(merge);
    }

    @Override
    public List<UserVO> list(String login, String loginPattern,
                              Integer role, Integer status,
                              String sortField, boolean sortOrderAsc,
                              Integer take, Integer skip) {
        CriteriaBuilder cb = criteriaBuilder();
        CriteriaQuery<User> cq = cb.createQuery(User.class);
        Root<User> from = cq.from(User.class);

        Predicate[] predicates = CriteriaHelper.userListPredicates(cb, from, ofNullable(login), ofNullable(loginPattern), ofNullable(role), ofNullable(status));
        cq.where(predicates);
        CriteriaHelper.order(cb, cq, from, ofNullable(sortField), Boolean.TRUE.equals(sortOrderAsc));

        TypedQuery<User> query = createQuery(cq);
        cacheQuery(query, of(CacheConfig.refresh()));
        ofNullable(take).ifPresent(query::setMaxResults);
        ofNullable(skip).ifPresent(query::setFirstResult);
        return query.getResultList().stream().map(User::convertToVo).collect(Collectors.toList());
    }

    @Override
    public long count(String login, String loginPattern, Integer role, Integer status) {
        final CriteriaBuilder cb = criteriaBuilder();
        final CriteriaQuery<Long> criteria = cb.createQuery(Long.class);
        final Root<User> from = criteria.from(User.class);

        final Predicate[] predicates = CriteriaHelper.userListPredicates(cb, from,
                ofNullable(login), ofNullable(loginPattern), ofNullable(role), ofNullable(status));

        criteria.where(predicates);
        criteria.select(cb.count(from));
        return count(criteria);
    }

    private Optional<UserVO> optionalUserConvertToVo(Optional<User> login) {
        if (login.isPresent()) {
            return Optional.ofNullable(User.convertToVo(login.get()));
        }
        return Optional.empty();
    }

}
