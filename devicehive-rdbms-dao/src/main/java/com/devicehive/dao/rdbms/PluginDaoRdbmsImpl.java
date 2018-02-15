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
import com.devicehive.dao.PluginDao;
import com.devicehive.model.Plugin;
import com.devicehive.model.enums.PluginStatus;
import com.devicehive.vo.PluginVO;
import org.springframework.stereotype.Repository;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.stream.Collectors;

import static com.devicehive.model.Plugin.convertToVo;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

@Repository
public class PluginDaoRdbmsImpl extends RdbmsGenericDao implements PluginDao {

    @Override
    public PluginVO find(Long id) {
        Plugin plugin = find(Plugin.class, id);
        return convertToVo(plugin);
    }

    @Override
    public List<PluginVO> findByStatus(PluginStatus status) {
        @SuppressWarnings("unchecked")
        List<Plugin> plugins = createNamedQuery("Plugin.findByStatus", of(CacheConfig.refresh()))
                .setParameter("status", status)
                .getResultList();
        
        return plugins.stream()
                .map(Plugin::convertToVo)
                .collect(Collectors.toList());
    }

    @Override
    public PluginVO findByTopic(String topicName) {
        Plugin plugin = (Plugin) createNamedQuery("Plugin.findByTopic", of(CacheConfig.refresh()))
                .setParameter("topicName", topicName)
                .getSingleResult();

        return convertToVo(plugin);
    }

    @Override
    public PluginVO findByName(String pluginName) {
        try {
            Plugin plugin = (Plugin) createNamedQuery("Plugin.findByName", of(CacheConfig.refresh()))
                    .setParameter("name", pluginName)
                    .getSingleResult();
            return convertToVo(plugin);
        } catch (NoResultException ex) {
            return null;
        }
    }

    @Override
    public void persist(PluginVO plugin) {
        Plugin entity = Plugin.convertToEntity(plugin);
        super.persist(entity);
        plugin.setId(entity.getId());
    }

    @Override
    public PluginVO merge(PluginVO existing) {
        Plugin entity = Plugin.convertToEntity(existing);
        Plugin merge = super.merge(entity);
        return convertToVo(merge);
    }

    @Override
    public int deleteById(long id) {
        return createNamedQuery("Plugin.deleteById", of(CacheConfig.bypass()))
                .setParameter("id", id)
                .executeUpdate();
    }

    @Override
    public List<PluginVO> list(String name, String namePattern, String topicName, Integer status, Long userId,
                               String sortField, boolean sortOrderAsc, Integer take, Integer skip, HivePrincipal principal) {
        final CriteriaBuilder cb = criteriaBuilder();
        final CriteriaQuery<Plugin> criteria = cb.createQuery(Plugin.class);
        final Root<Plugin> from = criteria.from(Plugin.class);

        final Predicate [] predicates = CriteriaHelper.pluginListPredicates(cb, from,
                ofNullable(name), ofNullable(namePattern), ofNullable(topicName), ofNullable(status), ofNullable(userId),
                ofNullable(principal));

        criteria.where(predicates);
        CriteriaHelper.order(cb, criteria, from, ofNullable(sortField), sortOrderAsc);

        final TypedQuery<Plugin> query = createQuery(criteria);
        cacheQuery(query, of(CacheConfig.refresh()));
        ofNullable(take).ifPresent(query::setMaxResults);
        ofNullable(skip).ifPresent(query::setFirstResult);
        List<Plugin> resultList = query.getResultList();
        return resultList.stream().map(Plugin::convertToVo).collect(Collectors.toList());
    }

    @Override
    public long count(String name, String namePattern, String topicName, Integer status, Long userId,
                               HivePrincipal principal) {
        final CriteriaBuilder cb = criteriaBuilder();
        final CriteriaQuery<Long> criteria = cb.createQuery(Long.class);
        final Root<Plugin> from = criteria.from(Plugin.class);

        final Predicate [] predicates = CriteriaHelper.pluginListPredicates(cb, from,
                ofNullable(name), ofNullable(namePattern), ofNullable(topicName), ofNullable(status), ofNullable(userId),
                ofNullable(principal));

        criteria.where(predicates);
        criteria.select(cb.count(from));
        return count(criteria);
    }

}
