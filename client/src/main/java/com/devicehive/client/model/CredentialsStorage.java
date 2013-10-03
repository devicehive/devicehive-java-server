package com.devicehive.client.model;


public class CredentialsStorage {

    private String id;
    private String credentials;
    private Role role;

    public CredentialsStorage(String id) {
        role = Role.KEY;
        this.id = id;
    }

    public CredentialsStorage(String id, String credentials, Role role) {
        if (role.equals(Role.KEY) && credentials != null) {
            throw new IllegalArgumentException("No credentials allowed for key!");
        }
        this.id = id;
        this.role = role;
        this.credentials = credentials;
    }

    public String getId() {
        return id;
    }

    public String getCredentials() {
        return credentials;
    }

    public Role getRole() {
        return role;
    }

    public static enum Role {
        USER,
        DEVICE,
        KEY
    }
}
