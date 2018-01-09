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
import com.devicehive.dao.DeviceDao;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceType;
import com.devicehive.model.Network;
import com.devicehive.vo.DeviceVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

@Repository
public class DeviceDaoRdbmsImpl extends RdbmsGenericDao implements DeviceDao {

    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public DeviceVO findById(String id) {
        Device deviceEntity = createNamedQuery(Device.class, "Device.findById", Optional.of(CacheConfig.get()))
                .setParameter("deviceId", id)
                .getResultList()
                .stream().findFirst().orElse(null);
        return Device.convertToVo(deviceEntity);
    }

    @Override
    public void persist(DeviceVO vo) {
        Device device = Device.convertToEntity(vo);
        if (device.getNetwork() != null) {
            device.setNetwork(reference(Network.class, device.getNetwork().getId()));
        }
        if (device.getDeviceType() != null) {
            device.setDeviceType(reference(DeviceType.class, device.getDeviceType().getId()));
        }
        super.persist(device);
        vo.setId(device.getId());
    }


    @Override
    public DeviceVO merge(DeviceVO vo) {
        Device device = Device.convertToEntity(vo);
        if (device.getNetwork() != null) {
            device.setNetwork(reference(Network.class, device.getNetwork().getId()));
        }
        if (device.getDeviceType() != null) {
            device.setDeviceType(reference(DeviceType.class, device.getDeviceType().getId()));
        }
        Device merged = super.merge(device);
        return Device.convertToVo(merged);
    }

    @Override
    public int deleteById(String deviceId) {
        return createNamedQuery("Device.deleteById", Optional.empty())
                .setParameter("deviceId", deviceId)
                .executeUpdate();
    }

    @Override
    public List<DeviceVO> getDeviceList(List<String> deviceIds, HivePrincipal principal) {
        final CriteriaBuilder cb = criteriaBuilder();
        final CriteriaQuery<Device> criteria = cb.createQuery(Device.class);
        final Root<Device> from = criteria.from(Device.class);
        final Predicate[] predicates = CriteriaHelper.deviceListPredicates(cb, from, deviceIds, Optional.ofNullable(principal));
        criteria.where(predicates);
        final TypedQuery<Device> query = createQuery(criteria);
        CacheHelper.cacheable(query);
        return query.getResultList().stream().map(Device::convertToVo).collect(Collectors.toList());
    }

    @Override
    public List<DeviceVO> list(String name, String namePattern, Long networkId, String networkName,
                                String sortField, boolean sortOrderAsc, Integer take,
                                Integer skip, HivePrincipal principal) {
        final CriteriaBuilder cb = criteriaBuilder();
        final CriteriaQuery<Device> criteria = cb.createQuery(Device.class);
        final Root<Device> from = criteria.from(Device.class);

        final Predicate [] predicates = CriteriaHelper.deviceListPredicates(cb, from, ofNullable(name), ofNullable(namePattern), ofNullable(networkId), ofNullable(networkName),
                ofNullable(principal));

        criteria.where(predicates);
        CriteriaHelper.order(cb, criteria, from, ofNullable(sortField), sortOrderAsc);

        final TypedQuery<Device> query = createQuery(criteria);
        cacheQuery(query, of(CacheConfig.refresh()));
        ofNullable(take).ifPresent(query::setMaxResults);
        ofNullable(skip).ifPresent(query::setFirstResult);
        List<Device> resultList = query.getResultList();
        return resultList.stream().map(Device::convertToVo).collect(Collectors.toList());
    }

    @Override
    public long count(String name, String namePattern, Long networkId, String networkName, HivePrincipal principal) {
        final CriteriaBuilder cb = criteriaBuilder();
        final CriteriaQuery<Long> criteria = cb.createQuery(Long.class);
        final Root<Device> from = criteria.from(Device.class);

        final Predicate[] predicates = CriteriaHelper.deviceCountPredicates(cb, from,
                ofNullable(name), ofNullable(namePattern), ofNullable(networkId), ofNullable(networkName),
                ofNullable(principal));

        criteria.where(predicates);
        criteria.select(cb.count(from));
        return count(criteria);
    }


}
