package com.devicehive.dao.rdbms;

import com.devicehive.dao.AccessKeyDao;
import com.devicehive.model.AccessKey;
import com.devicehive.model.User;
import com.devicehive.vo.AccessKeyVO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

@Profile({"rdbms"})
@Repository
public class AccessKeyDaoRdbmsImpl extends RdbmsGenericDao implements AccessKeyDao {

    @Override
    public AccessKeyVO getById(Long keyId, Long userId) {
        return AccessKey.convert(createNamedQuery(AccessKey.class, "AccessKey.getById", Optional.of(CacheConfig.refresh()))
                .setParameter("userId", userId)
                .setParameter("accessKeyId", keyId)
                .getResultList()
                .stream().findFirst().orElse(null));
    }

    @Override
    public Optional<AccessKeyVO> getByKey(String key) {
        Optional<AccessKey> accessKeyOptional = createNamedQuery(AccessKey.class, "AccessKey.getByKey", Optional.of(CacheConfig.get()))
                .setParameter("someKey", key)
                .getResultList().stream().findFirst();
        if (accessKeyOptional.isPresent()) {
            AccessKeyVO vo = AccessKey.convert(accessKeyOptional.get());
            return Optional.of(vo);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<AccessKeyVO> getByUserAndLabel(User user, String label) {
        Optional<AccessKey> accessKeyOptional = createNamedQuery(AccessKey.class, "AccessKey.getByUserAndLabel", Optional.<CacheConfig>empty())
                .setParameter("userId", user.getId())
                .setParameter("label", label)
                .getResultList().stream().findFirst();
        if (accessKeyOptional.isPresent()) {
            AccessKeyVO vo = AccessKey.convert(accessKeyOptional.get());
            return Optional.of(vo);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public int deleteByIdAndUser(Long keyId, Long userId) {
        return createNamedQuery("AccessKey.deleteByIdAndUser", Optional.<CacheConfig>empty())
                .setParameter("accessKeyId", keyId)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    @Override
    public int deleteById(Long keyId) {
        return createNamedQuery("AccessKey.deleteById", Optional.<CacheConfig>empty())
                .setParameter("accessKeyId", keyId)
                .executeUpdate();
    }

    @Override
    public int deleteOlderThan(Date date) {
        return createNamedQuery("AccessKey.deleteOlderThan", Optional.<CacheConfig>empty())
                .setParameter("expirationDate", date)
                .executeUpdate();
    }

    @Override
    public AccessKeyVO find(Long id) {
        return AccessKey.convert(find(AccessKey.class, id));
    }

    @Override
    public void persist(AccessKeyVO accessKey) {
        AccessKey key = AccessKey.convert(accessKey);
        super.persist(key);
        accessKey.setId(key.getId());
    }

    @Override
    public AccessKeyVO merge(AccessKeyVO existing) {
        return AccessKey.convert(super.merge(AccessKey.convert(existing)));
    }

    @Override
    public List<AccessKeyVO> list(Long userId, String label,
                                  String labelPattern, Integer type,
                                  String sortField, Boolean sortOrderAsc,
                                  Integer take, Integer skip) {
        CriteriaBuilder cb = criteriaBuilder();
        CriteriaQuery<AccessKey> cq = cb.createQuery(AccessKey.class);
        Root<AccessKey> from = cq.from(AccessKey.class);

        Predicate[] predicates = CriteriaHelper.accessKeyListPredicates(cb, from, userId, ofNullable(label), ofNullable(labelPattern), ofNullable(type));
        cq.where(predicates);
        CriteriaHelper.order(cb, cq, from, ofNullable(sortField), Boolean.TRUE.equals(sortOrderAsc));

        TypedQuery<AccessKey> query = createQuery(cq);
        ofNullable(skip).ifPresent(query::setFirstResult);
        ofNullable(take).ifPresent(query::setMaxResults);
        cacheQuery(query, of(CacheConfig.bypass()));
        return query.getResultList().stream().map(AccessKey::convert).collect(Collectors.toList());
    }

}
