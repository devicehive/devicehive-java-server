package com.devicehive.vo;

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

import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.HiveEntity;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.model.enums.UserRole;
import com.devicehive.model.enums.UserStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Date;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

public class UserVO implements HiveEntity {

    @SerializedName("id")
    @JsonPolicyDef({COMMAND_TO_CLIENT, COMMAND_TO_DEVICE, USER_PUBLISHED, USERS_LISTED, USER_SUBMITTED})
    private Long id;

    @SerializedName("login")
    @NotNull(message = "login field cannot be null.")
    @Size(min = 1, max = 128, message = "Field cannot be empty. The length of login should not be more than 128 " +
            "symbols.")
    @JsonPolicyDef({USER_PUBLISHED, USERS_LISTED})
    private String login;

    private String passwordHash;

    private String passwordSalt;

    private Integer loginAttempts;

    @SerializedName("role")
    @JsonPolicyDef({USER_PUBLISHED, USERS_LISTED})
    private UserRole role;

    @SerializedName("status")
    @JsonPolicyDef({USER_PUBLISHED, USERS_LISTED})
    private UserStatus status;

    @SerializedName("lastLogin")
    @JsonPolicyDef({USER_PUBLISHED, USERS_LISTED, USER_SUBMITTED})
    private Date lastLogin;

    @SerializedName("googleLogin")
    @JsonPolicyDef({USER_PUBLISHED, USERS_LISTED, USER_SUBMITTED})
    private String googleLogin;

    @SerializedName("facebookLogin")
    @JsonPolicyDef({USER_PUBLISHED, USERS_LISTED, USER_SUBMITTED})
    private String facebookLogin;

    @SerializedName("githubLogin")
    @JsonPolicyDef({USER_PUBLISHED, USERS_LISTED, USER_SUBMITTED})
    private String githubLogin;

    @SerializedName("data")
    @JsonPolicyDef({USER_PUBLISHED, USERS_LISTED, USER_SUBMITTED})
    private JsonStringWrapper data;

    /**
     * @return true, if user is admin
     */
    public boolean isAdmin() {
        return UserRole.ADMIN.equals(role);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public Date getLastLogin() {
        return ObjectUtils.cloneIfPossible(lastLogin);
    }

    public void setLastLogin(Date lastLogin) {
        this.lastLogin = ObjectUtils.cloneIfPossible(lastLogin);
    }

    public String getPasswordSalt() {
        return passwordSalt;
    }

    public void setPasswordSalt(String passwordSalt) {
        this.passwordSalt = passwordSalt;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Integer getLoginAttempts() {
        return loginAttempts;
    }

    public void setLoginAttempts(Integer loginAttempts) {
        this.loginAttempts = loginAttempts;
    }

    public String getGoogleLogin() {
        return googleLogin;
    }

    public void setGoogleLogin(String googleLogin) {
        this.googleLogin = StringUtils.trim(googleLogin);
    }

    public String getFacebookLogin() {
        return facebookLogin;
    }

    public void setFacebookLogin(String facebookLogin) {
        this.facebookLogin = StringUtils.trim(facebookLogin);
    }

    public String getGithubLogin() {
        return githubLogin;
    }

    public void setGithubLogin(String githubLogin) {
        this.githubLogin = StringUtils.trim(githubLogin);
    }

    public JsonStringWrapper getData() {
        return data;
    }

    public void setData(JsonStringWrapper data) {
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        UserVO user = (UserVO) o;

        return id != null && id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }

}
