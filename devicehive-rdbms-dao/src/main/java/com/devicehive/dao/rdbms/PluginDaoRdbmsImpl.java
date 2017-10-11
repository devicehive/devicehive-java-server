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
import com.devicehive.vo.PluginVO;
import org.springframework.stereotype.Repository;

import static java.util.Optional.of;

@Repository
public class PluginDaoRdbmsImpl extends RdbmsGenericDao implements PluginDao {

    @Override
    public PluginVO find(Long id) {
        Plugin plugin = find(Plugin.class, id);
        return Plugin.convertToVo(plugin);
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
        return Plugin.convertToVo(merge);
    }

    @Override
    public int deleteById(long id) {
        return createNamedQuery("Plugin.deleteById", of(CacheConfig.bypass()))
                .setParameter("id", id)
                .executeUpdate();
    }

}
