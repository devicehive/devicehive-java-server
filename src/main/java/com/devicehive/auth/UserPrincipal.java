package com.devicehive.auth;


import com.devicehive.model.User;

import java.security.Principal;

public class UserPrincipal implements Principal {

    private User user;

    public UserPrincipal(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    @Override
    public String getName() {
        return user.getLogin();
    }
}
