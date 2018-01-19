package com.devicehive.dao.rdbms;

/*
 * #%L
 * DeviceHive Dao RDBMS Implementation
 * %%
 * Copyright (C) 2016 - 2017 DataArt
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
import com.devicehive.dao.DeviceTypeDao;
import com.devicehive.model.DeviceType;
import com.devicehive.model.User;
import com.devicehive.vo.DeviceTypeVO;
import com.devicehive.vo.DeviceTypeWithUsersAndDevicesVO;
import com.devicehive.vo.UserVO;
import org.springframework.stereotype.Repository;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

@Repository
public class DeviceTypeDaoRdbmsImpl extends RdbmsGenericDao implements DeviceTypeDao {

    @Override
    public List<DeviceTypeVO> findByName(String name) {
        List<DeviceType> result = createNamedQuery(DeviceType.class, "DeviceType.findByName", Optional.of(CacheConfig.get()))
                .setParameter("name", name)
                .getResultList();
        Stream<DeviceTypeVO> objectStream = result.stream().map(DeviceType::convertDeviceType);
        return objectStream.collect(Collectors.toList());
    }

    @Override
    public void persist(DeviceTypeVO newDeviceType) {
        DeviceType deviceType = DeviceType.convert(newDeviceType);
        super.persist(deviceType);
        newDeviceType.setId(deviceType.getId());
    }

    @Override
    public List<DeviceTypeWithUsersAndDevicesVO> getDeviceTypesByIdsAndUsers(Long idForFiltering, Set<Long> deviceTypeIds, Set<Long> permittedDeviceTypes) {
        TypedQuery<DeviceType> query = createNamedQuery(DeviceType.class, "DeviceType.getDeviceTypesByIdsAndUsers",
                Optional.of(CacheConfig.get()))
                .setParameter("userId", idForFiltering)
                .setParameter("deviceTypeIds", deviceTypeIds)
                .setParameter("permittedDeviceTypes", permittedDeviceTypes);
        List<DeviceType> result = query.getResultList();
        Stream<DeviceTypeWithUsersAndDevicesVO> objectStream = result.stream().map(DeviceType::convertWithDevicesAndUsers);
        return objectStream.collect(Collectors.toList());
    }

    @Override
    public int deleteById(long id) {
        return createNamedQuery("DeviceType.deleteById", Optional.empty())
                .setParameter("id", id)
                .executeUpdate();
    }

    @Override
    public DeviceTypeVO find(Long deviceTypeId) {
        DeviceType deviceType = find(DeviceType.class, deviceTypeId);
        return deviceType != null ? DeviceType.convertDeviceType(deviceType) : null;
    }

    @Override
    public DeviceTypeVO merge(DeviceTypeVO existing) {
        DeviceType deviceType = find(DeviceType.class, existing.getId());
        deviceType.setName(existing.getName());
        deviceType.setDescription(existing.getDescription());
        deviceType.setEntityVersion(existing.getEntityVersion());
        super.merge(deviceType);
        return existing;
    }

    @Override
    public void assignToDeviceType(DeviceTypeVO deviceType, UserVO user) {
        assert deviceType != null && deviceType.getId() != null;
        assert user != null && user.getId() != null;
        DeviceType existing = find(DeviceType.class, deviceType.getId());
        User userReference = reference(User.class, user.getId());
        if (existing.getUsers() == null) {
            existing.setUsers(new HashSet<>());
        }
        existing.getUsers().add(userReference);
        super.merge(existing);
    }

    @Override
    public List<DeviceTypeVO> list(String name, String namePattern, String sortField, boolean sortOrderAsc, Integer take, Integer skip, Optional<HivePrincipal> principal) {
        CriteriaBuilder cb = criteriaBuilder();
        CriteriaQuery<DeviceType> criteria = cb.createQuery(DeviceType.class);
        Root<DeviceType> from = criteria.from(DeviceType.class);

        Predicate[] nameAndPrincipalPredicates = CriteriaHelper.deviceTypeListPredicates(cb, from, ofNullable(name), ofNullable(namePattern), principal);
        criteria.where(nameAndPrincipalPredicates);

        CriteriaHelper.order(cb, criteria, from, ofNullable(sortField), sortOrderAsc);

        TypedQuery<DeviceType> query = createQuery(criteria);
        cacheQuery(query, of(CacheConfig.refresh()));
        ofNullable(take).ifPresent(query::setMaxResults);
        ofNullable(skip).ifPresent(query::setFirstResult);
        List<DeviceType> result = query.getResultList();
        Stream<DeviceTypeVO> objectStream = result.stream().map(DeviceType::convertDeviceType);
        return objectStream.collect(Collectors.toList());
    }

    @Override
    public long count(String name, String namePattern, Optional<HivePrincipal> principal) {
        CriteriaBuilder cb = criteriaBuilder();
        CriteriaQuery<Long> criteria = cb.createQuery(Long.class);
        Root<DeviceType> from = criteria.from(DeviceType.class);

        Predicate[] nameAndPrincipalPredicates = CriteriaHelper.deviceTypeListPredicates(cb, from,
                ofNullable(name), ofNullable(namePattern), principal);

        criteria.where(nameAndPrincipalPredicates);
        criteria.select(cb.count(from));
        return count(criteria);
    }

    @Override
    public List<DeviceTypeVO> listAll() {
        return createNamedQuery(DeviceType.class, "DeviceType.findAll", of(CacheConfig.refresh()))
                .getResultList().stream()
                .map(DeviceType::convertDeviceType).collect(Collectors.toList());
    }

    @Override
    public Optional<DeviceTypeVO> findFirstByName(String name) {
        return findByName(name).stream().findFirst();
    }

    @Override
    public Optional<DeviceTypeWithUsersAndDevicesVO> findWithUsers(long deviceTypeId) {
        List<DeviceType> deviceTypes = createNamedQuery(DeviceType.class, "DeviceType.findWithUsers", Optional.of(CacheConfig.refresh()))
                .setParameter("id", deviceTypeId)
                .getResultList();
        return deviceTypes.isEmpty() ? Optional.empty() : Optional.ofNullable(DeviceType.convertWithDevicesAndUsers(deviceTypes.get(0)));
    }

    @Override
    public Optional<DeviceTypeVO> findDefault(Set<Long> deviceTypeIds) {
        return createNamedQuery(DeviceType.class, "DeviceType.findOrderedByIdWithPermission", Optional.of(CacheConfig.refresh()))
                .setParameter("permittedDeviceTypes", deviceTypeIds)
                .getResultList().stream()
                .findFirst()
                .map(DeviceType::convertDeviceType);
    }
}
