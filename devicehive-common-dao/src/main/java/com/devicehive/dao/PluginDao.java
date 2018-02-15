package com.devicehive.dao;

/*
 * #%L
 * DeviceHive Common Dao Interfaces
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
import com.devicehive.model.enums.PluginStatus;
import com.devicehive.vo.PluginVO;

import java.util.List;

public interface PluginDao {

    PluginVO find(Long id);

    List<PluginVO> findByStatus(PluginStatus status);
    
    PluginVO findByTopic(String topicName);

    PluginVO findByName(String pluginName);

    void persist(PluginVO pluginVO);

    PluginVO merge(PluginVO existing);

    int deleteById(long id);

    List<PluginVO> list(String name, String namePattern, String topicName, Integer status, Long userId,
                        String sortField, boolean sortOrderAsc, Integer take, Integer skip, HivePrincipal principal);

    long count(String name, String namePattern, String topicName, Integer status, Long userId, HivePrincipal principal);
}
