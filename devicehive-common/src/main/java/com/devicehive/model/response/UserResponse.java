package com.devicehive.model.response;

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
import com.devicehive.vo.UserWithNetworkVO;
import com.google.gson.annotations.SerializedName;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.ObjectUtils;

import javax.persistence.Column;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

/**
 * @author Nikolay Loboda
 * @since 30.07.13
 */
public class UserResponse implements HiveEntity {

    private static final long serialVersionUID = 7947516851877980861L;
    @SerializedName("id")
    @JsonPolicyDef({COMMAND_TO_CLIENT, COMMAND_TO_DEVICE, USER_PUBLISHED, USERS_LISTED})
    private Long id;

    @SerializedName("login")
    @JsonPolicyDef({USER_PUBLISHED, USERS_LISTED})
    private String login;

    @Column(name = "login_attempts")
    @ApiModelProperty(hidden = true)
    private Integer loginAttempts;

    @SerializedName("role")
    @JsonPolicyDef({USER_PUBLISHED, USERS_LISTED})
    @ApiModelProperty(dataType = "int", allowableValues = "0, 1")
    private UserRole role;

    @SerializedName("status")
    @JsonPolicyDef({USER_PUBLISHED, USERS_LISTED})
    @ApiModelProperty(dataType = "int", allowableValues = "0, 1, 2")
    private UserStatus status;

    @JsonPolicyDef({USER_PUBLISHED})
    private Set<UserNetworkResponse> networks;

    @SerializedName("lastLogin")
    @JsonPolicyDef({USER_PUBLISHED, USERS_LISTED})
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastLogin = new Date(0);

    @SerializedName("data")
    @JsonPolicyDef({USER_PUBLISHED, USERS_LISTED, USER_SUBMITTED})
    private JsonStringWrapper data;

    @SerializedName("introReviewed")
    @JsonPolicyDef({USER_PUBLISHED, USERS_LISTED, USER_SUBMITTED})
    private Boolean introReviewed;

    public static UserResponse createFromUser(UserWithNetworkVO u) {
        UserResponse response = new UserResponse();
        response.setId(u.getId());
        response.setLogin(u.getLogin());
        response.setLoginAttempts(u.getLoginAttempts());
        response.setRole(u.getRole());
        response.setStatus(u.getStatus());
        response.networks = new HashSet<>();
        response.networks.addAll(u.getNetworks().stream().map(UserNetworkResponse::fromNetwork).collect(Collectors.toList()));
        response.setData(u.getData());
        response.setLastLogin(u.getLastLogin());
        response.setIntroReviewed(u.getIntroReviewed());
        return response;
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

    public Set<UserNetworkResponse> getNetworks() {
        return networks;
    }

    public void setNetworks(Set<UserNetworkResponse> networks) {
        this.networks = networks;
    }

    public Integer getLoginAttempts() {
        return loginAttempts;
    }

    public void setLoginAttempts(Integer loginAttempts) {
        this.loginAttempts = loginAttempts;
    }

    public JsonStringWrapper getData() {
        return data;
    }

    public void setData(JsonStringWrapper data) {
        this.data = data;
    }

    public Boolean getIntroReviewed() {
        return introReviewed;
    }

    public void setIntroReviewed(Boolean introReviewed) {
        this.introReviewed = introReviewed;
    }
}
