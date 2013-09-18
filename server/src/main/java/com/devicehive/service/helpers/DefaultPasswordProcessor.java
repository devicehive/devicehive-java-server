package com.devicehive.service.helpers;

import com.devicehive.configuration.Constants;
import org.apache.commons.codec.binary.Base64;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * this one uses the same scheme as .net server
 * TODO investigate maybe it makes sense to replace it with some key-stretching scheme (scrypt, PBKDF2 or bcrypt)
 */

public class DefaultPasswordProcessor implements PasswordProcessor {

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generateSalt() {
        byte[] saltBytes = new byte[18]; // .net server uses 18 bytes salt
        secureRandom.nextBytes(saltBytes);
        return Base64.encodeBase64String(saltBytes);
    }

    /**
     * TODO do we need timing-safe implementation?
     *
     * @param password
     * @param salt
     * @param hash
     * @return
     */
    @Override
    public boolean checkPassword(String password, String salt, String hash) {
        return hash.equals(hashPassword(password, salt));
    }

    /**
     * Implements self-made hash scheme.
     *
     * @param password
     * @param salt
     * @return
     */
    @Override
    public String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest((salt + password).getBytes(Constants.UTF8));
            return Base64.encodeBase64String(hashBytes);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

}
