package com.devicehive.dao.rdbms;

import com.devicehive.dao.DeviceClassDao;
import com.devicehive.model.DeviceClass;
import com.devicehive.model.DeviceClassEquipment;
import com.devicehive.vo.DeviceClassEquipmentVO;
import com.devicehive.vo.DeviceClassWithEquipmentVO;
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
    public DeviceClassWithEquipmentVO find(long id) {
        DeviceClass entity = find(DeviceClass.class, id);
        return DeviceClass.convertToVo(entity);
    }

    @Override
    public DeviceClassWithEquipmentVO persist(DeviceClassWithEquipmentVO deviceClass) {
        DeviceClass dc = DeviceClass.convertWithEquipmentToEntity(deviceClass);
        super.persist(dc);
        deviceClass.setId(dc.getId());
        return DeviceClass.convertToVo(dc);
    }

    @Override
    public DeviceClassWithEquipmentVO merge(DeviceClassWithEquipmentVO deviceClass) {
        DeviceClass entity = DeviceClass.convertWithEquipmentToEntity(deviceClass);

        List<DeviceClassEquipment> existingEquipments = createNamedQuery(DeviceClassEquipment.class, "Equipment.getByDeviceClass", Optional.empty())
                .setParameter("deviceClass", entity)
                .getResultList();

        Set<String> codes = new HashSet<>();
        if (entity.getEquipment() != null) {
            for (DeviceClassEquipment equipment : entity.getEquipment()) {
                codes.add(equipment.getCode());
                equipment.setDeviceClass(entity);
            }
        }

        DeviceClass merged = super.merge(entity);

        for (DeviceClassEquipment equipment : existingEquipments) {
            if (!codes.contains(equipment.getCode())) {
                remove(equipment);
            }
        }

        return DeviceClass.convertToVo(merged);
    }

    @Override
    public List<DeviceClassWithEquipmentVO> getDeviceClassList(String name, String namePattern, String sortField, Boolean sortOrderAsc, Integer take, Integer skip) {
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
        Stream<DeviceClassWithEquipmentVO> objectStream = resultList.stream().map(DeviceClass::convertToVo);
        return objectStream.collect(Collectors.toList());
    }

    @Override
    public DeviceClassWithEquipmentVO findByName(@NotNull String name) {
        DeviceClass deviceClass = createNamedQuery(DeviceClass.class, "DeviceClass.findByName", Optional.of(CacheConfig.get()))
                .setParameter("name", name)
                .getResultList()
                .stream().findFirst().orElse(null);
        return DeviceClass.convertToVo(deviceClass);
    }

    @Override
    public DeviceClassEquipmentVO findDeviceClassEquipment(@NotNull long deviceClassId, @NotNull long equipmentId) {
        DeviceClassEquipment equipment = createNamedQuery(DeviceClassEquipment.class, "Equipment.getByDeviceClassAndId", Optional.of(CacheConfig.get()))
                .setParameter("id", equipmentId)
                .setParameter("deviceClassId", deviceClassId)
                .getResultList()
                .stream().findFirst().orElse(null);
        return DeviceClassEquipment.convertDeviceClassEquipment(equipment);
    }
}
