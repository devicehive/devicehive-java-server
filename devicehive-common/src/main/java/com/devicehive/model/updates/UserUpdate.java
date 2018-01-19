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
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import javax.ws.rs.DefaultValue;
import java.util.Optional;

public class UserUpdate implements HiveEntity {

    private static final long serialVersionUID = -8353201743020153250L;

    @Size(min = 1, max = 128, message = "Field login cannot be empty. The length of login should not be more than 128 symbols.")
    private String login;

    @ApiModelProperty(value = "0 for 'ADMIN', 1 for 'CLIENT'")
    @Max(value = 1, message = "The value of role should not be more than 1.")
    @Min(value = 0, message = "The value of role should not be less than 0.")
    private Integer role;

    @ApiModelProperty(value = "0 for 'ACTIVE', 1 for 'LOCKED_OUT', 2 for 'DISABLED'")
    @Max(value = 3, message = "The value of status should not be more than 3.")
    @Min(value = 0, message = "The value of status should not be less than 0.")
    private Integer status;

    @Size(max = 128, message = "The length of password should not be more than 128 symbols.")
    private String password;

    private JsonStringWrapper data;

    private Boolean introReviewed;

    @ApiModelProperty(hidden = true)
    private Boolean allDeviceTypesAvailable;

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

    public Optional<JsonStringWrapper> getData() {
        return Optional.ofNullable(data);
    }

    public void setData(JsonStringWrapper data) {
        this.data = data;
    }

    public Optional<Boolean> getIntroReviewed() {
        return Optional.ofNullable(introReviewed);
    }

    public void setIntroReviewed(Boolean introReviewed) {
        this.introReviewed = introReviewed;
    }

    public Boolean getAllDeviceTypesAvailable() {
        return allDeviceTypesAvailable;
    }

    public void setAllDeviceTypesAvailable(Boolean allDeviceTypesAvailable) {
        this.allDeviceTypesAvailable = allDeviceTypesAvailable;
    }

    @ApiModelProperty(hidden = true)
    public UserRole getRoleEnum() {
        return getRole().map(UserRole::getValueForIndex).orElse(null);
    }

    @ApiModelProperty(hidden = true)
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
        if (introReviewed != null) {
            result.setIntroReviewed(introReviewed);
        }
        if (allDeviceTypesAvailable != null) {
            result.setAllDeviceTypesAvailable(allDeviceTypesAvailable);
        }
        result.setStatus(getStatusEnum());
        result.setRole(getRoleEnum());
        return result;
    }
}
