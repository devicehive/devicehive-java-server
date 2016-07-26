package com.devicehive.dao.rdbms;

import com.devicehive.dao.CacheConfig;
import com.devicehive.dao.CacheHelper;
import com.devicehive.dao.CriteriaHelper;
import com.devicehive.dao.DeviceClassDao;
import com.devicehive.model.DeviceClass;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.Optional;

import static java.util.Optional.ofNullable;

@Profile({"rdbms"})
@Repository
public class DeviceClassDaoRdbmsImpl extends RdbmsGenericDao implements DeviceClassDao {

    @Override
    public void remove(long id) {
        DeviceClass deviceClass = find(id);
        remove(deviceClass);
    }

    @Override
    public DeviceClass find(long id) {
        return find(DeviceClass.class, id);
    }

    @Override
    public void persist(DeviceClass deviceClass) {
        super.persist(deviceClass);
    }

    @Override
    public DeviceClass merge(DeviceClass deviceClass) {
        return super.merge(deviceClass);
    }

    @Override
    public List<DeviceClass> getDeviceClassList(String name, String namePattern, String sortField, Boolean sortOrderAsc, Integer take, Integer skip) {
        final CriteriaBuilder cb = criteriaBuilder();
        final CriteriaQuery<DeviceClass> criteria = cb.createQuery(DeviceClass.class);
        final Root<DeviceClass> from = criteria.from(DeviceClass.class);

        final Predicate[] predicates = CriteriaHelper.deviceClassListPredicates(cb, from, ofNullable(name),
                ofNullable(namePattern));
        criteria.where(predicates);
        CriteriaHelper.order(cb, criteria, from, ofNullable(sortField), Boolean.TRUE.equals(sortOrderAsc));

        final TypedQuery<DeviceClass> query = createQuery(criteria);
        ofNullable(take).ifPresent(query::setMaxResults);
        ofNullable(skip).ifPresent(query::setFirstResult);

        CacheHelper.cacheable(query);
        return query.getResultList();
    }

    @Override
    public DeviceClass findByName(String name) {
        return createNamedQuery(DeviceClass.class, "DeviceClass.findByName", Optional.of(CacheConfig.get()))
                .setParameter("name", name)
                .getResultList()
                .stream().findFirst().orElse(null);
    }
}
