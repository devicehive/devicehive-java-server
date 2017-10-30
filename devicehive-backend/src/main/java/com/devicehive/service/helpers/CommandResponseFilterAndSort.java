package com.devicehive.service.helpers;

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

import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;

import java.util.*;

public final class CommandResponseFilterAndSort {

    private CommandResponseFilterAndSort() {

    }

    public static <T> List<T> orderAndLimit(List<T> deviceCommands,
                                            Comparator<T> cmp, Boolean reverse,
                                            Integer skip, Integer take) {
        if (cmp != null) {
            Collections.sort(deviceCommands, cmp);
        }
        if (Boolean.FALSE.equals(reverse)) {
            Collections.reverse(deviceCommands);
        }
        
        return subList(deviceCommands, Optional.ofNullable(skip).orElse(0),
                Optional.ofNullable(take).orElse(deviceCommands.size()));
    }

    private static <T> List<T> subList(List<T> deviceCommands, Integer skip, Integer take) {
        if (skip < 0 || take <= 0 || skip >= deviceCommands.size()) {
            return Collections.emptyList();
        }
        int end = (int) Math.min(deviceCommands.size(), (long) skip + take);
        return deviceCommands.subList(skip, end);
    }

    public static Comparator<DeviceCommand> buildDeviceCommandComparator(String field) {
        if ("timestamp".equalsIgnoreCase(field)) {
            return (o1, o2) -> o2.getTimestamp().compareTo(o1.getTimestamp());
        } else if ("status".equalsIgnoreCase(field)) {
            return (o1, o2) -> {
                String o2Status = o2.getStatus() == null ? "" : o2.getStatus();
                return o2Status.compareTo(o1.getStatus());
            };
        } else if ("command".equalsIgnoreCase(field)) {
            return (o1, o2) -> {
                String o2Command = o2.getCommand() == null ? "" : o2.getCommand();
                return o2Command.compareTo(o1.getCommand());
            };
        }
        return null;
    }

    public static Comparator<DeviceNotification> buildDeviceNotificationComparator(String field) {
        if ("timestamp".equalsIgnoreCase(field)) {
            return (o1, o2) -> o2.getTimestamp().compareTo(o1.getTimestamp());
        } else if ("notification".equalsIgnoreCase(field)) {
            return (o1, o2) -> {
                String o2Status = o2.getNotification() == null ? "" : o2.getNotification();
                return o2Status.compareTo(o1.getNotification());
            };
        }
        return null;
    }

    public static Integer getTotal(Integer skip, Integer take) {
        Integer updatedSkip = Optional.ofNullable(skip).orElse(0);
        Integer updatedTake = Optional.ofNullable(take).orElse(0);

        return updatedTake.equals(0) ? 0 : updatedSkip + updatedTake;
    }
}
