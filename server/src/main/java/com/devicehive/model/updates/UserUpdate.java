package com.devicehive.model.updates;

import com.devicehive.model.HiveEntity;
import com.devicehive.model.NullableWrapper;
import com.devicehive.model.User;
import com.devicehive.model.enums.UserRole;
import com.devicehive.model.enums.UserStatus;

public class UserUpdate implements HiveEntity {

    private static final long serialVersionUID = -8353201743020153250L;
    private NullableWrapper<String> login;
    private NullableWrapper<Integer> role;
    private NullableWrapper<Integer> status;
    private NullableWrapper<String> password;
    private NullableWrapper<String> googleLogin;
    private NullableWrapper<String> facebookLogin;
    private NullableWrapper<String> githubLogin;

    public NullableWrapper<String> getLogin() {
        return login;
    }

    public void setLogin(NullableWrapper<String> login) {
        this.login = login;
    }

    public NullableWrapper<Integer> getRole() {
        return role;
    }

    public void setRole(NullableWrapper<Integer> role) {
        this.role = role;
    }

    public NullableWrapper<Integer> getStatus() {
        return status;
    }

    public void setStatus(NullableWrapper<Integer> status) {
        this.status = status;
    }

    public NullableWrapper<String> getPassword() {
        return password;
    }

    public void setPassword(NullableWrapper<String> password) {
        this.password = password;
    }

    public NullableWrapper<String> getGoogleLogin() {
        return googleLogin;
    }

    public void setGoogleLogin(NullableWrapper<String> googleLogin) {
        this.googleLogin = googleLogin;
    }

    public NullableWrapper<String> getFacebookLogin() {
        return facebookLogin;
    }

    public void setFacebookLogin(NullableWrapper<String> facebookLogin) {
        this.facebookLogin = facebookLogin;
    }

    public NullableWrapper<String> getGithubLogin() {
        return githubLogin;
    }

    public void setGithubLogin(NullableWrapper<String> githubLogin) {
        this.githubLogin = githubLogin;
    }

    public UserRole getRoleEnum() {
        if (role == null) {
            return null;
        }
        Integer roleValue = role.getValue();
        if (roleValue == null) {
            return null;
        }
        return UserRole.values()[roleValue];
    }

    public UserStatus getStatusEnum() {
        if (status == null) {
            return null;
        }
        Integer statusValue = status.getValue();
        if (statusValue == null) {
            return null;
        }
        return UserStatus.values()[statusValue];
    }

    public User convertTo() {
        User result = new User();
        if (login != null) {
            result.setLogin(login.getValue());
        }
        if (googleLogin != null) {
            result.setGoogleLogin(googleLogin.getValue());
        }
        if (facebookLogin != null) {
            result.setFacebookLogin(facebookLogin.getValue());
        }
        if (githubLogin != null) {
            result.setGithubLogin(githubLogin.getValue());
        }
        result.setStatus(getStatusEnum());
        result.setRole(getRoleEnum());
        return result;
    }
}
