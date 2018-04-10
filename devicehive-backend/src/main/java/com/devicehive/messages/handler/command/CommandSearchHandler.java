package com.devicehive.messages.handler.command;

/*
 * #%L
 * DeviceHive Backend Logic
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

import com.devicehive.model.DeviceCommand;
import com.devicehive.model.rpc.CommandSearchRequest;
import com.devicehive.model.rpc.CommandSearchResponse;
import com.devicehive.service.HazelcastService;
import com.devicehive.service.helpers.CommandResponseFilterAndSort;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static com.devicehive.service.helpers.CommandResponseFilterAndSort.buildDeviceCommandComparator;
import static com.devicehive.service.helpers.CommandResponseFilterAndSort.getTotal;
import static com.devicehive.service.helpers.CommandResponseFilterAndSort.orderAndLimit;

@Component
public class CommandSearchHandler implements RequestHandler {

    private HazelcastService hazelcastService;

    @Autowired
    public void setHazelcastService(HazelcastService hazelcastService) {
        this.hazelcastService = hazelcastService;
    }

    @Override
    public Response handle(Request request) {
        CommandSearchRequest searchRequest = (CommandSearchRequest) request.getBody();

        CommandSearchResponse payload = searchRequest.getId() != null && !StringUtils.isEmpty(searchRequest.getDeviceId())
                ? searchSingleCommandByDeviceAndId(searchRequest.getId(), searchRequest.getDeviceId(), searchRequest.isReturnUpdated())
                : searchMultipleCommands(searchRequest);

        return Response.newBuilder()
                .withBody(payload)
                .buildSuccess();
    }

    private CommandSearchResponse searchSingleCommandByDeviceAndId(long id, String deviceId, boolean returnUpdated) {
        final CommandSearchResponse commandSearchResponse = new CommandSearchResponse();
        final List<DeviceCommand> commands = hazelcastService.find(id, deviceId, returnUpdated, DeviceCommand.class)
                .map(Collections::singletonList)
                .orElse(Collections.emptyList());

        commandSearchResponse.setCommands(commands);
        return commandSearchResponse;
    }

    private CommandSearchResponse searchMultipleCommands(CommandSearchRequest searchRequest) {
        final CommandSearchResponse commandSearchResponse = new CommandSearchResponse();
        final Collection<DeviceCommand> commands = hazelcastService.find(
                searchRequest.getDeviceIds(),
                searchRequest.getNames(),
                getTotal(searchRequest.getSkip(), searchRequest.getTake()),
                searchRequest.getTimestampStart(),
                searchRequest.getTimestampEnd(),
                searchRequest.isReturnUpdated(),
                searchRequest.getStatus(),
                DeviceCommand.class);
        
        final Comparator<DeviceCommand> comparator = buildDeviceCommandComparator(searchRequest.getSortField());
        
        String sortOrder = searchRequest.getSortOrder();
        final Boolean reverse = sortOrder == null ? null : "desc".equalsIgnoreCase(sortOrder);

        final List<DeviceCommand> sortedDeviceCommands = orderAndLimit(new ArrayList<>(commands),
                        comparator, reverse, searchRequest.getSkip(), searchRequest.getTake());
        
        commandSearchResponse.setCommands(new ArrayList<>(sortedDeviceCommands));
        return commandSearchResponse;
    }
    
}
