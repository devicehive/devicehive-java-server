package com.devicehive.client.context;


import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class HivePrincipal {

    private Pair<String, String> user;

    private Pair<String, String> device;

    private String accessKey;

    private HivePrincipal(Pair<String, String> user, Pair<String, String> device, String accessKey) {
        this.user = user;
        this.device = device;
        this.accessKey = accessKey;
    }

    public Pair<String, String> getUser() {
        return user;
    }

    public Pair<String, String> getDevice() {
        return device;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public static HivePrincipal createUser(String login, String password) {
        return new HivePrincipal(ImmutablePair.of(login, password), null, null);
    }

    public static HivePrincipal createDevice(String id, String key) {
        return new HivePrincipal(null, ImmutablePair.of(id, key), null);
    }

    public static HivePrincipal createAccessKey(String key) {
        return new HivePrincipal(null, null, key);
    }
}
