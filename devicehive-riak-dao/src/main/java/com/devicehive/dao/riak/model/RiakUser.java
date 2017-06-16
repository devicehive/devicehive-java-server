package com.devicehive.dao.riak.model;

/*
 * #%L
 * DeviceHive Dao Riak Implementation
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

import com.basho.riak.client.api.annotations.RiakIndex;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.model.enums.UserRole;
import com.devicehive.model.enums.UserStatus;
import com.devicehive.vo.UserVO;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Date;

public class RiakUser {

    private Long id;

    private String login;

    private String passwordHash;

    private String passwordSalt;

    private Integer loginAttempts;

    private UserRole role;

    private UserStatus status;

    private Date lastLogin;

    private long entityVersion;

    private JsonStringWrapper data;

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

    public long getEntityVersion() {
        return entityVersion;
    }

    public void setEntityVersion(long entityVersion) {
        this.entityVersion = entityVersion;
    }

    public JsonStringWrapper getData() {
        return data;
    }

    public void setData(JsonStringWrapper data) {
        this.data = data;
    }

    //Riak indexes
    @RiakIndex(name = "login")
    public String getLoginSi() {
        return login;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RiakUser user = (RiakUser) o;

        return id != null && id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }

    public static UserVO convertToVo(RiakUser dc) {
        UserVO vo = null;
        if (dc != null) {
            vo = new UserVO();
            vo.setData(dc.getData());
            vo.setId(dc.getId());
            vo.setLastLogin(dc.getLastLogin());
            vo.setLogin(dc.getLogin());
            vo.setLoginAttempts(dc.getLoginAttempts());
            //TODO [rafa] ??? vo.setNetworks(dc.getN);
            vo.setPasswordHash(dc.getPasswordHash());
            vo.setPasswordSalt(dc.getPasswordSalt());
            vo.setRole(dc.getRole());
            vo.setStatus(dc.getStatus());
        }
        return vo;
    }

    public static RiakUser convertToEntity(UserVO dc) {
        RiakUser vo = null;
        if (dc != null) {
            vo = new RiakUser();
            vo.setData(dc.getData());
            vo.setId(dc.getId());
            vo.setLastLogin(dc.getLastLogin());
            vo.setLogin(dc.getLogin());
            vo.setLoginAttempts(dc.getLoginAttempts());
            //TODO [rafa] ??? vo.setNetworks(dc.getN);
            vo.setPasswordHash(dc.getPasswordHash());
            vo.setPasswordSalt(dc.getPasswordSalt());
            vo.setRole(dc.getRole());
            vo.setStatus(dc.getStatus());
        }
        return vo;
    }

}
