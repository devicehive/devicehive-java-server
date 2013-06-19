package com.devicehive.service;

import com.devicehive.model.User;

import java.io.Serializable;

public interface PasswordService extends Serializable {

    String generateSalt();

    String hashPassword(String password, String salt);

    boolean checkPassword(String password, String salt, String hash);
}
