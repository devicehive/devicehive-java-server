package com.devicehive.model.rpc;

/*
 * #%L
 * DeviceHive Common Module
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

import com.devicehive.model.eventbus.Filter;
import com.devicehive.shim.api.Action;
import com.devicehive.shim.api.Body;

import java.util.Map;
import java.util.Objects;

public class ListSubscribeResponse extends Body {

    private Map<Long, Filter> subscriptions;

    public ListSubscribeResponse(Map<Long, Filter> subscriptions) {
        super(Action.LIST_SUBSCRIBE_RESPONSE);
        this.subscriptions = subscriptions;

    }

    public Map<Long, Filter> getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(Map<Long, Filter> subscriptions) {
        this.subscriptions = subscriptions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ListSubscribeResponse)) return false;
        if (!super.equals(o)) return false;

        ListSubscribeResponse that = (ListSubscribeResponse) o;
        return Objects.equals(subscriptions, that.subscriptions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), subscriptions);
    }

    @Override
    public String toString() {
        return "ListSubscribeResponse{" +
                "subscriptions='" + subscriptions + '\'' +
                '}';
    }
}
