package com.devicehive.model.updates;

import com.devicehive.model.HiveEntity;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.model.User;
import com.devicehive.model.enums.UserRole;
import com.devicehive.model.enums.UserStatus;
import com.devicehive.vo.UserVO;

import java.util.Optional;

public class UserUpdate implements HiveEntity {

    private static final long serialVersionUID = -8353201743020153250L;
    private Optional<String> login;
    private Optional<Integer> role;
    private Optional<Integer> status;
    private Optional<String> password;
    private Optional<String> oldPassword;
    private Optional<String> googleLogin;
    private Optional<String> facebookLogin;
    private Optional<String> githubLogin;
    private Optional<JsonStringWrapper> data;

    public Optional<String> getLogin() {
        return login;
    }

    public void setLogin(Optional<String> login) {
        this.login = login;
    }

    public Optional<Integer> getRole() {
        return role;
    }

    public void setRole(Optional<Integer> role) {
        this.role = role;
    }

    public Optional<Integer> getStatus() {
        return status;
    }

    public void setStatus(Optional<Integer> status) {
        this.status = status;
    }

    public Optional<String> getPassword() {
        return password;
    }

    public void setPassword(Optional<String> password) {
        this.password = password;
    }

    public Optional<String> getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(Optional<String> oldPassword) {
        this.oldPassword = oldPassword;
    }

    public Optional<String> getGoogleLogin() {
        return googleLogin;
    }

    public void setGoogleLogin(Optional<String> googleLogin) {
        this.googleLogin = googleLogin;
    }

    public Optional<String> getFacebookLogin() {
        return facebookLogin;
    }

    public void setFacebookLogin(Optional<String> facebookLogin) {
        this.facebookLogin = facebookLogin;
    }

    public Optional<String> getGithubLogin() {
        return githubLogin;
    }

    public void setGithubLogin(Optional<String> githubLogin) {
        this.githubLogin = githubLogin;
    }

    public Optional<JsonStringWrapper> getData() {
        return data;
    }

    public void setData(Optional<JsonStringWrapper> data) {
        this.data = data;
    }

    public UserRole getRoleEnum() {
        if(role != null) {
            return role.map(UserRole::getValueForIndex).orElse(null);
        }
        return null;
    }

    public UserStatus getStatusEnum() {
        if(status != null) {
            return status.map(UserStatus::getValueForIndex).orElse(null);
        }
        return null;
    }

    public UserVO convertTo() {
        UserVO result = new UserVO();
        if (login != null) {
            result.setLogin(login.orElse(null));
        }
        if (googleLogin != null) {
            result.setGoogleLogin(googleLogin.orElse(null));
        }
        if (facebookLogin != null) {
            result.setFacebookLogin(facebookLogin.orElse(null));
        }
        if (githubLogin != null) {
            result.setGithubLogin(githubLogin.orElse(null));
        }
        if (data != null) {
            result.setData(data.orElse(null));
        }
        result.setStatus(getStatusEnum());
        result.setRole(getRoleEnum());
        return result;
    }
}
