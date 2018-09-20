package com.devicehive.util;

/*
 * #%L
 * DeviceHive Frontend Logic
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

import com.devicehive.model.enums.UserStatus;
import com.devicehive.model.rpc.ListUserRequest;
import com.devicehive.model.updates.UserUpdate;
import com.devicehive.service.UserService;
import com.devicehive.vo.UserVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class UserUnlockScheduler {

    private static final Logger logger = LoggerFactory.getLogger(UserUnlockScheduler.class);

    private final UserService userService;

    @Autowired
    public UserUnlockScheduler(UserService userService) {
        this.userService = userService;
    }

    @Scheduled(fixedRateString = "${user.unlock.interval.ms:1800000}")
    public void unlockUsers() {
        ListUserRequest listUserRequest = new ListUserRequest();
        listUserRequest.setStatus(UserStatus.LOCKED_OUT.getValue());

        userService.list(listUserRequest).thenAccept(users -> {
            UserUpdate userUpdate = new UserUpdate();
            userUpdate.setStatus(UserStatus.ACTIVE.getValue());

            users.stream().map(UserVO::getId).forEach(id -> userService.updateUser(id, userUpdate));
            logger.debug("Successfully unlocked {} users", users.size());
        });
    }
}
