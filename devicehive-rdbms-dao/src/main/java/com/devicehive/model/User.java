package com.devicehive.model;

/*
 * #%L
 * DeviceHive Dao RDBMS Implementation
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
import com.devicehive.model.enums.UserRole;
import com.devicehive.model.enums.UserStatus;
import com.devicehive.vo.DeviceTypeVO;
import com.devicehive.vo.UserVO;
import com.devicehive.vo.UserWithDeviceTypeVO;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.ObjectUtils;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

@Entity(name = "User")
@Table(name = "\"dh_user\"")
@NamedQueries({
        @NamedQuery(name = "User.findByName", query = "select u from User u where u.login = :login and u.status <> 3"), //TODO this actually finds by login, not name - consider refactoring
        @NamedQuery(name = "User.hasAccessToNetwork", query = "select count(distinct u) from User u join u.networks n where u.id = :user and n = :network"),
        @NamedQuery(name = "User.hasAccessToDevice", query = "select count(distinct n) from Network n join n.devices d join n.users u where u.id = :user and d.deviceId = :deviceId"),
        @NamedQuery(name = "User.getWithNetworksById", query = "select u from User u left join fetch u.networks where u.id = :id"),
        @NamedQuery(name = "User.getWithDeviceTypesById", query = "select u from User u left join fetch u.deviceTypes where u.id = :id"),
        @NamedQuery(name = "User.deleteById", query = "delete from User u where u.id = :id")
})
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class User implements HiveEntity {
    private static final long serialVersionUID = -8980491502416082011L;
    private static final String LOGIN_SIZE_MESSAGE = "Field cannot be empty. The length of login should be from 3 " +
            "to 128 symbols.";
    private static final String LOGIN_PATTERN_MESSAGE = "Login can contain only lowercase or uppercase letters, " +
            "numbers, and some special symbols (_@.)";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @SerializedName("id")
    @JsonPolicyDef({COMMAND_TO_CLIENT, COMMAND_TO_DEVICE, USER_PUBLISHED, USERS_LISTED, USER_SUBMITTED})
    private Long id;

    @Column
    @SerializedName("login")
    @NotNull(message = "login field cannot be null.")
    @Size(min = 3, max = 128, message = LOGIN_SIZE_MESSAGE)
    @Pattern(regexp = "^[\\w@.-]+$", message = LOGIN_PATTERN_MESSAGE)
    @JsonPolicyDef({USER_PUBLISHED, USERS_LISTED})
    private String login;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "password_salt")
    private String passwordSalt;

    @Column(name = "login_attempts")
    private Integer loginAttempts;

    @Column
    @SerializedName("role")
    @JsonPolicyDef({USER_PUBLISHED, USERS_LISTED})
    private UserRole role;

    @Column
    @SerializedName("status")
    @JsonPolicyDef({USER_PUBLISHED, USERS_LISTED})
    private UserStatus status;

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "users")
    @JsonPolicyDef({USER_PUBLISHED})
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<Network> networks;

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "users")
    @JsonPolicyDef({USER_PUBLISHED})
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<DeviceType> deviceTypes;

    @Column(name = "last_login")
    @SerializedName("lastLogin")
    @JsonPolicyDef({USER_PUBLISHED, USERS_LISTED, USER_SUBMITTED})
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastLogin;

    @SerializedName("data")
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "jsonString", column = @Column(name = "data"))
    })
    @JsonPolicyDef({USER_PUBLISHED, USERS_LISTED, USER_SUBMITTED})
    private JsonStringWrapper data;

    @Column(name = "intro_reviewed")
    @SerializedName("introReviewed")
    @JsonPolicyDef({USER_PUBLISHED, USERS_LISTED, USER_SUBMITTED})
    private Boolean introReviewed;

    @Column(name = "all_device_types_available")
    @SerializedName("allDeviceTypesAvailable")
    @JsonPolicyDef({USER_PUBLISHED, USERS_LISTED, USER_SUBMITTED})
    private Boolean allDeviceTypesAvailable;

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

    public Set<Network> getNetworks() {
        return networks;
    }

    public void setNetworks(Set<Network> networks) {
        this.networks = networks;
    }

    public Set<DeviceType> getDeviceTypes() {
        return deviceTypes;
    }

    public void setDeviceTypes(Set<DeviceType> deviceTypes) {
        this.deviceTypes = deviceTypes;
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

    public Boolean getAllDeviceTypesAvailable() {
        return allDeviceTypesAvailable;
    }

    public void setAllDeviceTypesAvailable(Boolean allDeviceTypesAvailable) {
        this.allDeviceTypesAvailable = allDeviceTypesAvailable;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        User user = (User) o;

        return id != null && id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }


    public static UserVO convertToVo(User dc) {
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
            vo.setIntroReviewed(dc.getIntroReviewed());
            vo.setAllDeviceTypesAvailable(dc.getAllDeviceTypesAvailable());
        }
        return vo;
    }

    public static User convertToEntity(UserVO dc) {
        User vo = null;
        if (dc != null) {
            vo = new User();
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
            vo.setIntroReviewed(dc.getIntroReviewed());
            vo.setAllDeviceTypesAvailable(dc.getAllDeviceTypesAvailable());
        }
        return vo;
    }

    public static User convertToEntity(UserWithDeviceTypeVO dc) {
        User vo = null;
        if (dc != null) {
            vo = new User();
            vo.setData(dc.getData());
            vo.setId(dc.getId());
            vo.setLastLogin(dc.getLastLogin());
            vo.setLogin(dc.getLogin());
            vo.setLoginAttempts(dc.getLoginAttempts());
            vo.setPasswordHash(dc.getPasswordHash());
            vo.setPasswordSalt(dc.getPasswordSalt());
            vo.setRole(dc.getRole());
            vo.setStatus(dc.getStatus());
            vo.setIntroReviewed(dc.getIntroReviewed());
            vo.setAllDeviceTypesAvailable(dc.getAllDeviceTypesAvailable());

            vo.setDeviceTypes(new HashSet<>());

            for (DeviceTypeVO deviceTypeVO : dc.getDeviceTypes()) {
                DeviceType deviceType = DeviceType.convert(deviceTypeVO);
                vo.getDeviceTypes().add(deviceType);
            }
        }
        return vo;
    }

}
