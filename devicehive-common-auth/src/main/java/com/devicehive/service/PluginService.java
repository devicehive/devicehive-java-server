package com.devicehive.service;

/*
 * #%L
 * DeviceHive Java Server Common business logic
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

import com.devicehive.configuration.Messages;
import com.devicehive.dao.PluginDao;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.enums.PluginStatus;
import com.devicehive.util.HiveValidator;
import com.devicehive.vo.PluginVO;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

@Component
public class PluginService {
    private static final Logger logger = LoggerFactory.getLogger(PluginService.class);

    private final HiveValidator hiveValidator;
    private final PluginDao pluginDao;

    @Autowired
    public PluginService(HiveValidator hiveValidator,
            PluginDao pluginDao) {
        this.hiveValidator = hiveValidator;
        this.pluginDao = pluginDao;
    }
    
    @Transactional
    public PluginVO create(PluginVO pluginVO) {
        pluginDao.persist(pluginVO);
        
        return pluginVO;
    }

    @Transactional
    public PluginVO update(PluginVO pluginVO) {
        return pluginDao.merge(pluginVO);
    }

    @Transactional
    public List<PluginVO> findByStatus(PluginStatus status) {
        return pluginDao.findByStatus(status);
    }

    @Transactional
    public PluginVO findByTopic(String topicName) {
        try {
            return pluginDao.findByTopic(topicName);
        } catch (Exception e) {
            return null;
        }
    }

    @Transactional
    public PluginVO findByName(String pluginName) {
        return pluginDao.findByName(pluginName);
    }

    @Transactional
    public boolean delete(long id) {
        int result = pluginDao.deleteById(id);
        return result > 0;
    }

}
