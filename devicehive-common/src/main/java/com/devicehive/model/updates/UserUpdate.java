package com.devicehive.model.updates;

/*
 * #%L
 * DeviceHive Common Dao Interfaces
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

import com.devicehive.model.HiveEntity;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.model.enums.UserRole;
import com.devicehive.model.enums.UserStatus;
import com.devicehive.vo.UserVO;

import java.util.Optional;

public class UserUpdate implements HiveEntity {

    private static final long serialVersionUID = -8353201743020153250L;
    private String login;
    private Integer role;
    private Integer status;
    private String password;
    private String oldPassword;
    private JsonStringWrapper data;

    public Optional<String> getLogin() {
        return Optional.ofNullable(login);
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public Optional<Integer> getRole() {
        return Optional.ofNullable(role);
    }

    public void setRole(Integer role) {
        this.role = role;
    }

    public Optional<Integer> getStatus() {
        return Optional.ofNullable(status);
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Optional<String> getPassword() {
        return Optional.ofNullable(password);
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Optional<String> getOldPassword() {
        return Optional.ofNullable(oldPassword);
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public Optional<JsonStringWrapper> getData() {
        return Optional.ofNullable(data);
    }

    public void setData(JsonStringWrapper data) {
        this.data = data;
    }

    public UserRole getRoleEnum() {
        return getRole().map(UserRole::getValueForIndex).orElse(null);
    }

    public UserStatus getStatusEnum() {
        return getStatus().map(UserStatus::getValueForIndex).orElse(null);
    }

    public UserVO convertTo() {
        UserVO result = new UserVO();
        if (login != null) {
            result.setLogin(login);
        }
        if (data != null) {
            result.setData(data);
        }
        result.setStatus(getStatusEnum());
        result.setRole(getRoleEnum());
        return result;
    }
}
