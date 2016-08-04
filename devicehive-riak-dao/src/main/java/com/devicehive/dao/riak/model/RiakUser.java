package com.devicehive.dao.riak.model;

import com.basho.riak.client.api.annotations.RiakIndex;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.model.enums.UserRole;
import com.devicehive.model.enums.UserStatus;
import com.devicehive.vo.DeviceClassVO;
import com.devicehive.vo.DeviceVO;
import com.devicehive.vo.NetworkVO;
import com.devicehive.vo.UserVO;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

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

    private String googleLogin;

    private String facebookLogin;

    private String githubLogin;

    private long entityVersion;

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

    @RiakIndex(name = "googleLogin")
    public String getGoogleLoginSi() {
        return googleLogin;
    }

    @RiakIndex(name = "facebookLogin")
    public String getFacebookLoginSi() {
        return facebookLogin;
    }

    @RiakIndex(name = "githubLogin")
    public String getGithubLoginSi() {
        return githubLogin;
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
            vo.setFacebookLogin(dc.getFacebookLogin());
            vo.setGithubLogin(dc.getGithubLogin());
            vo.setGoogleLogin(dc.getGoogleLogin());
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
            vo.setFacebookLogin(dc.getFacebookLogin());
            vo.setGithubLogin(dc.getGithubLogin());
            vo.setGoogleLogin(dc.getGoogleLogin());
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
