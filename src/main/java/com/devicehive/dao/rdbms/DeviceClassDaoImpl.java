package com.devicehive.dao.rdbms;

import com.devicehive.dao.CacheConfig;
import com.devicehive.dao.CacheHelper;
import com.devicehive.dao.CriteriaHelper;
import com.devicehive.dao.DeviceClassDao;
import com.devicehive.model.DeviceClass;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
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
public class DeviceClassDaoImpl extends GenericDaoImpl implements DeviceClassDao {
    @Override
    public DeviceClass findByNameAndVersion(String name, String version) {
        return createNamedQuery(DeviceClass.class, "DeviceClass.findByNameAndVersion", Optional.of(CacheConfig.get()))
                .setParameter("name", name)
                .setParameter("version", version)
                .getResultList()
                .stream().findFirst().orElse(null);
    }

    @Override
    public boolean isExist(long id) {
        return isExist(DeviceClass.class, id);
    }

    @Override
    public DeviceClass getReference(long id) {
        return getReference(DeviceClass.class, id);
    }

    @Override
    public void remove(DeviceClass reference) {
        super.remove(reference);
    }

    @Override
    public DeviceClass find(long id) {
        return find(DeviceClass.class, id);
    }

    @Override
    public void refresh(DeviceClass stored, LockModeType lockModeType) {
        super.refresh(stored, lockModeType);
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
    public List<DeviceClass> getDeviceClassList(String name, String namePattern, String version, String sortField, Boolean sortOrderAsc, Integer take, Integer skip) {
        final CriteriaBuilder cb = criteriaBuilder();
        final CriteriaQuery<DeviceClass> criteria = cb.createQuery(DeviceClass.class);
        final Root<DeviceClass> from = criteria.from(DeviceClass.class);

        final Predicate[] predicates = CriteriaHelper.deviceClassListPredicates(cb, from, ofNullable(name),
                ofNullable(namePattern), ofNullable(version));
        criteria.where(predicates);
        CriteriaHelper.order(cb, criteria, from, ofNullable(sortField), Boolean.TRUE.equals(sortOrderAsc));

        final TypedQuery<DeviceClass> query = createQuery(criteria);
        ofNullable(take).ifPresent(query::setMaxResults);
        ofNullable(skip).ifPresent(query::setFirstResult);

        CacheHelper.cacheable(query);
        return query.getResultList();
    }
}
