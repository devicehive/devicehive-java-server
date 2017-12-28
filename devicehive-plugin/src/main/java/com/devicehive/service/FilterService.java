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

import com.devicehive.auth.HivePrincipal;
import com.devicehive.model.eventbus.Filter;
import com.devicehive.model.query.PluginReqisterQuery;
import com.devicehive.vo.DeviceVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.devicehive.model.converters.SetHelper.toLongSet;
import static com.devicehive.model.converters.SetHelper.toStringSet;

@Service
public class FilterService {

    private static final Logger logger = LoggerFactory.getLogger(FilterService.class);

    private final BaseDeviceService deviceService;

    @Autowired
    public FilterService(BaseDeviceService deviceService) {
        this.deviceService = deviceService;
    }

    public Set<Filter> createFilters(PluginReqisterQuery query) {
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Set<Filter> filters;
        if (query.getDeviceId() != null) {
            DeviceVO device = deviceService.findByIdWithPermissionsCheck(query.getDeviceId(), principal);
            if (device == null) {
                logger.error("Could not find device with id={}", query.getDeviceId());
            }
            if (query.getNames() != null) {
                filters = toStringSet(query.getNames()).stream().map(name ->
                        new Filter(device.getNetworkId(), device.getDeviceTypeId(), query.getDeviceId(), null, name))
                        .collect(Collectors.toSet());
            } else {
                filters = Collections.singleton(new Filter(device.getNetworkId(), device.getDeviceTypeId(), query.getDeviceId(), null, null));
            }
        } else {
            if (query.getNetworkIds() == null && query.getDeviceTypeIds() == null) {
                if (query.getNames() != null) {
                    filters = toStringSet(query.getNames()).stream().map(name ->
                            new Filter(null, null, null, null, name))
                            .collect(Collectors.toSet());
                } else {
                    filters = Collections.singleton(new Filter());
                }
            } else {
                Set<Long> networks = toLongSet(query.getNetworkIds());
                if (networks.isEmpty()) {
                    networks = principal.getNetworkIds();
                }
                Set<Long> deviceTypes = toLongSet(query.getDeviceTypeIds());
                if (deviceTypes.isEmpty()) {
                    deviceTypes = principal.getDeviceTypeIds();
                }
                final Set<Long> finalDeviceTypes = deviceTypes;
                filters = networks.stream()
                        .flatMap(network -> finalDeviceTypes.stream().flatMap(deviceType -> {
                            if (query.getNames() != null) {
                                return toStringSet(query.getNames()).stream().map(name ->
                                        new Filter(network, deviceType, null, null, name)
                                );
                            } else {
                                return Stream.of(new Filter(network, deviceType, null, null, null));
                            }
                        }))
                        .collect(Collectors.toSet());
            }
        }

        return filters;
    }
}
