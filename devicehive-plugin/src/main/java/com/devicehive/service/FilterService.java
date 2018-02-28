package com.devicehive.service;

/*
 * #%L
 * DeviceHive Frontend Logic
 * %%
 * Copyright (C) 2016 - 2017 DataArt
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

import com.devicehive.auth.HiveAuthentication;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.model.FilterEntity;
import com.devicehive.model.eventbus.Filter;
import com.devicehive.model.query.PluginReqisterQuery;
import com.devicehive.model.rpc.ListDeviceTypeRequest;
import com.devicehive.model.rpc.ListNetworkRequest;
import com.devicehive.model.rpc.PluginSubscribeRequest;
import com.devicehive.vo.DeviceTypeVO;
import com.devicehive.vo.DeviceVO;
import com.devicehive.vo.NetworkVO;
import com.devicehive.vo.PluginVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.devicehive.model.converters.SetHelper.toLongSet;
import static com.devicehive.model.converters.SetHelper.toStringSet;

@Service
public class FilterService {

    private static final Logger logger = LoggerFactory.getLogger(FilterService.class);

    private final BaseFilterService filterService;

    @Autowired
    public FilterService(BaseFilterService filterService) {
        this.filterService = filterService;
    }

    public PluginSubscribeRequest createPluginSubscribeRequest(String filter) {
        FilterEntity filterEntity = new FilterEntity(filter);

        PluginSubscribeRequest request = new PluginSubscribeRequest();
        request.setFilters(createFilters(filterEntity));
        request.setReturnCommands(filterEntity.isReturnCommands());
        request.setReturnUpdatedCommands(filterEntity.isReturnUpdatedCommands());
        request.setReturnNotifications(filterEntity.isReturnNotifications());

        return request;
    }

    private Set<Filter> createFilters(FilterEntity filterEntity) {
        final HiveAuthentication authentication = (HiveAuthentication) SecurityContextHolder.getContext().getAuthentication();
        return filterService.getFilterList(filterEntity.getDeviceId(), toLongSet(filterEntity.getNetworkIds()),
                toLongSet(filterEntity.getDeviceTypeIds()),
                null, toStringSet(filterEntity.getNames()), authentication);
    }
}
