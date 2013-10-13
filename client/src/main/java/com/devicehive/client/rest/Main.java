package com.devicehive.client.rest;


import com.devicehive.client.config.Preferences;
import com.devicehive.client.model.CredentialsStorage;
import com.devicehive.client.model.Device;
import com.devicehive.client.rest.controller.DeviceController;

public class Main {
    public static void main(String... args) {
        try {
            Preferences.setRestServerUrl("http://127.0.0.1:8080/hive/rest");
            Preferences.setCurrentUserInfoStorage(new CredentialsStorage("dhadmin", "dhadmin_#911", CredentialsStorage.Role.USER));
            DeviceController deviceController = new DeviceController();
            Device device = deviceController.getDevice("33335200-abe8-4f60-90c8-0d43c5f6c0f6");
            System.out.print(device.getName());
        } catch (Exception e) {
            System.out.print(e);
        }

    }

}
