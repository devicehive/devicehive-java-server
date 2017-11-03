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

import com.devicehive.dao.PluginDao;
import com.devicehive.model.Plugin;
import com.devicehive.model.enums.PluginStatus;
import com.devicehive.vo.PluginVO;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

import static com.devicehive.model.Plugin.convertToVo;
import static java.util.Optional.of;

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
        List<Plugin> plugins = createNamedQuery("Plugin.findByStatus", of(CacheConfig.get()))
                .setParameter("status", status)
                .getResultList();
        
        return plugins.stream()
                .map(Plugin::convertToVo)
                .collect(Collectors.toList());
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

}
