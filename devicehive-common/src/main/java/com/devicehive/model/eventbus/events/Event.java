package com.devicehive.model.eventbus.events;

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

import java.util.Collection;

public abstract class Event extends Body {

    public Event(Action action) {
        super(action);
    }

    /**
     * Returns applicable to this event filter.
     * For example, if event is device_notification { deviceId = a, notificationName = b },
     * then subscriptions on { deviceId = a, name = null } and { deviceId = a, name = b } will be returned,
     * but not { deviceId = a, name = c }, cause it is not applicable to this event.
     *
     * @return valid filter, for which this event can be routed.
     */
    public abstract Collection<Filter> getApplicableFilters();

}
