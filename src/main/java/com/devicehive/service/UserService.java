package com.devicehive.service;

import com.devicehive.dao.UserDAO;
import com.devicehive.model.User;

import javax.inject.Inject;
import javax.transaction.Transactional;

/**
 * Created with IntelliJ IDEA.
 * User: ssidorenko
 * Date: 21.06.13
 * Time: 12:52
 * To change this template use File | Settings | File Templates.
 */
public class UserService {


    @Inject
    private UserDAO userDAO;

    @Inject
    private PasswordService passwordService;

    /**
     * Tries to authenticate with given credentials
     * @param login
     * @param password
     * @return User object if authentication is successful or null if not
     */
    @Transactional
    public User authenticate(String login, String password) {
        User user = userDAO.findByLogin(login);
        if (user == null) {
            return null;
        }
        if (User.STATUS.Active.ordinal() != user.getStatus()) {
            return null;
        }
        if (!passwordService.checkPassword(password, user.getPasswordSalt(), user.getPasswordHash())) {
            userDAO.incrementLoginAttempts(user);
            return null;
        } else {
            return userDAO.finalizeLogin(user);
        }
    }

}
