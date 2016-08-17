package com.devicehive.messages.subscriptions;

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

import java.util.Set;

public class NotificationSubscriptionStorage extends AbstractStorage<String, NotificationSubscription> {

    public Set<NotificationSubscription> getByDeviceGuid(String guid) {
        return get(guid);
    }

    public synchronized void removeByDevice(String deviceGuid) {
        removeByEventSource(deviceGuid);
    }

}
