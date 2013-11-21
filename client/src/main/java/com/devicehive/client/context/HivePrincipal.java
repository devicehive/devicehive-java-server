package com.devicehive.client.context;


import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Principal storage. Save credentials of authenticated device or client.
 */
public class HivePrincipal {

    private Pair<String, String> user;
    private Pair<String, String> device;
    private String accessKey;

    private HivePrincipal(Pair<String, String> user, Pair<String, String> device, String accessKey) {
        this.user = user;
        this.device = device;
        this.accessKey = accessKey;
    }

    /**
     * Create new hive principal with user credentials.
     *
     * @param login    login
     * @param password password
     * @return new hive principal with user credentials
     */
    public static HivePrincipal createUser(String login, String password) {
        return new HivePrincipal(ImmutablePair.of(login, password), null, null);
    }

    /**
     * Create new hive principal with device credentials.
     *
     * @param id  device identifier
     * @param key device key
     * @return new hive principal with device credentials.
     */
    public static HivePrincipal createDevice(String id, String key) {
        return new HivePrincipal(null, ImmutablePair.of(id, key), null);
    }

    /**
     * Create new hive principal with access key credentials.
     *
     * @param key access key
     * @return new hive principal with access key credentials
     */
    public static HivePrincipal createAccessKey(String key) {
        return new HivePrincipal(null, null, key);
    }

    /**
     * Get user's credentials.
     *
     * @return pair of login and password
     */
    public Pair<String, String> getUser() {
        return user;
    }

    /**
     * Get device's credentials.
     *
     * @return pair of device identifier and key
     */
    public Pair<String, String> getDevice() {
        return device;
    }

    /**
     * Get access key's credentials.
     *
     * @return access key
     */
    public String getAccessKey() {
        return accessKey;
    }
}
