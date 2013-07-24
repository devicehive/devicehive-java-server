package com.devicehive.service.helpers;

import java.io.Serializable;

public interface PasswordProcessor extends Serializable {

    String generateSalt();

    String hashPassword(String password, String salt);

    boolean checkPassword(String password, String salt, String hash);
}
