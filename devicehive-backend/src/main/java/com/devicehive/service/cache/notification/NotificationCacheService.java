package com.devicehive.service.cache.notification;

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

import com.devicehive.model.DeviceNotification;
import com.devicehive.service.cache.CacheService;

import java.util.Collection;
import java.util.Date;

public interface NotificationCacheService extends CacheService<DeviceNotification> {

    Collection<DeviceNotification> find(final Collection<String> deviceIds,
                                        final Collection<String> notifications,
                                        final Integer take,
                                        final Date timestampFrom,
                                        final Date timestampTo);

    Collection<DeviceNotification> find(final String deviceId,
                                        final Collection<Long> networkIds,
                                        final Collection<Long> deviceTypeIds,
                                        final Collection<String> notifications,
                                        final Integer take,
                                        final Date timestampFrom);
}
