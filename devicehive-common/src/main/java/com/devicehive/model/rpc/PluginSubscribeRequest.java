package com.devicehive.model.rpc;

/*
 * #%L
 * DeviceHive Common Module
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

import com.devicehive.model.eventbus.Filter;
import com.devicehive.shim.api.Action;
import com.devicehive.shim.api.Body;

import java.util.Date;


public class PluginSubscribeRequest extends Body {

    private Long subscriptionId;
    private Filter filter;
    private Date timestamp;
    private String topicName;
    private boolean returnCommands;
    private boolean returnUpdatedCommands;
    private boolean returnNotifications;
    
    public PluginSubscribeRequest() {
        super(Action.PLUGIN_SUBSCRIBE_REQUEST);
    }

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(Long subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public boolean isReturnCommands() {
        return returnCommands;
    }

    public void setReturnCommands(boolean returnCommands) {
        this.returnCommands = returnCommands;
    }

    public boolean isReturnUpdatedCommands() {
        return returnUpdatedCommands;
    }

    public void setReturnUpdatedCommands(boolean returnUpdatedCommands) {
        this.returnUpdatedCommands = returnUpdatedCommands;
    }

    public boolean isReturnNotifications() {
        return returnNotifications;
    }

    public void setReturnNotifications(boolean returnNotifications) {
        this.returnNotifications = returnNotifications;
    }
}
