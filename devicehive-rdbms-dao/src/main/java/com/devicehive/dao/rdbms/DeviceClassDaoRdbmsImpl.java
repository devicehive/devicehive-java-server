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


import com.devicehive.dao.DeviceClassDao;
import com.devicehive.model.DeviceClass;
import com.devicehive.vo.DeviceClassVO;
import org.springframework.stereotype.Repository;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;

@Repository
public class DeviceClassDaoRdbmsImpl extends RdbmsGenericDao implements DeviceClassDao {

    @Override
    public void remove(long id) {
        DeviceClass deviceClass = find(DeviceClass.class, id);
        if (deviceClass != null) {
            remove(deviceClass);
        }
    }

    @Override
    public DeviceClassVO find(long id) {
        DeviceClass entity = find(DeviceClass.class, id);
        return DeviceClass.convertToVo(entity);
    }

    @Override
    public DeviceClassVO persist(DeviceClassVO deviceClass) {
        DeviceClass dc = DeviceClass.convertToEntity(deviceClass);
        super.persist(dc);
        deviceClass.setId(dc.getId());
        return DeviceClass.convertToVo(dc);
    }

    @Override
    public DeviceClassVO merge(DeviceClassVO deviceClass) {
        DeviceClass entity = DeviceClass.convertToEntity(deviceClass);

        DeviceClass merged = super.merge(entity);
        return DeviceClass.convertToVo(merged);
    }

    @Override
    public List<DeviceClassVO> list(String name, String namePattern, String sortField, boolean sortOrderAsc, Integer take, Integer skip) {
        final CriteriaBuilder cb = criteriaBuilder();
        final CriteriaQuery<DeviceClass> criteria = cb.createQuery(DeviceClass.class);
        final Root<DeviceClass> from = criteria.from(DeviceClass.class);

        final Predicate[] predicates = CriteriaHelper.deviceClassListPredicates(cb, from, ofNullable(name), ofNullable(namePattern));
        criteria.where(predicates);
        CriteriaHelper.order(cb, criteria, from, ofNullable(sortField), Boolean.TRUE.equals(sortOrderAsc));

        final TypedQuery<DeviceClass> query = createQuery(criteria);
        ofNullable(take).ifPresent(query::setMaxResults);
        ofNullable(skip).ifPresent(query::setFirstResult);

        CacheHelper.cacheable(query);
        List<DeviceClass> resultList = query.getResultList();
        Stream<DeviceClassVO> objectStream = resultList.stream().map(DeviceClass::convertToVo);
        return objectStream.collect(Collectors.toList());
    }

    @Override
    public DeviceClassVO findByName(@NotNull String name) {
        DeviceClass deviceClass = createNamedQuery(DeviceClass.class, "DeviceClass.findByName", Optional.of(CacheConfig.get()))
                .setParameter("name", name)
                .getResultList()
                .stream().findFirst().orElse(null);
        return DeviceClass.convertToVo(deviceClass);
    }
}
