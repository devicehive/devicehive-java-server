package com.devicehive.client.impl.context;


import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Principal storage. Save credentials of authenticated device or client.
 */
public class HivePrincipal {

    private final Pair<String, String> principal;

    private final Type type;

    private enum Type {
        USER, DEVICE, ACCESS_KEY
    }

    private HivePrincipal(Pair<String, String> principal, Type type) {
        this.principal = principal;
        this.type = type;
    }

    /**
     * Create new hive principal with user credentials.
     *
     * @param login    login
     * @param password password
     * @return new hive principal with user credentials
     */
    public static HivePrincipal createUser(String login, String password) {
        return new HivePrincipal(ImmutablePair.of(login, password), Type.USER);
    }

    /**
     * Create new hive principal with device credentials.
     *
     * @param id  device identifier
     * @param key device key
     * @return new hive principal with device credentials.
     */
    public static HivePrincipal createDevice(String id, String key) {
        return new HivePrincipal(ImmutablePair.of(id, key), Type.DEVICE);
    }

    /**
     * Create new hive principal with access key credentials.
     *
     * @param key access key
     * @return new hive principal with access key credentials
     */
    public static HivePrincipal createAccessKey(String key) {
        return new HivePrincipal(ImmutablePair.of((String) null, key), Type.ACCESS_KEY);
    }

    public Pair<String, String> getPrincipal() {
        return principal;
    }

    public boolean isUser() {
        return Type.USER.equals(this.type);
    }

    public boolean isDevice() {
        return Type.DEVICE.equals(this.type);
    }

    public boolean isAccessKey() {
        return Type.ACCESS_KEY.equals(this.type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HivePrincipal that = (HivePrincipal) o;

        if (principal != null ? !principal.equals(that.principal) : that.principal != null) return false;
        if (type != that.type) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = principal != null ? principal.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }
}
