package com.devicehive.service.helpers;


public interface PasswordProcessor {

    String generateSalt();

    String hashPassword(String password, String salt);

    boolean checkPassword(String password, String salt, String hash);
}
