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

import com.devicehive.shim.api.Action;
import com.devicehive.shim.api.Body;

import java.util.Objects;
import java.util.Set;

public class CommandUnsubscribeRequest extends Body {

    private Set<Long> subscriptionIds;

    public CommandUnsubscribeRequest(Set<Long> subscriptionIds) {
        super(Action.COMMAND_UNSUBSCRIBE_REQUEST);
        this.subscriptionIds = subscriptionIds;
    }

    public Set<Long> getSubscriptionIds() {
        return subscriptionIds;
    }

    public void setSubscriptionIds(Set<Long> subscriptionIds) {
        this.subscriptionIds = subscriptionIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CommandUnsubscribeRequest)) return false;
        if (!super.equals(o)) return false;

        CommandUnsubscribeRequest that = (CommandUnsubscribeRequest) o;
        return Objects.equals(subscriptionIds, that.subscriptionIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), subscriptionIds);
    }

    @Override
    public String toString() {
        return "CommandUnsubscribeRequest{" +
                "subscriptionIds='" + subscriptionIds + '\'' +
                '}';
    }
}
