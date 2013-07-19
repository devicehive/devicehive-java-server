package com.devicehive.websockets.json.strategies;


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class HiveAnnotations {


    @Retention(RetentionPolicy.RUNTIME)
    public @interface CommandFromClient {}

    @Retention(RetentionPolicy.RUNTIME)
    public @interface CommandToClient {}


    @Retention(RetentionPolicy.RUNTIME)
    public @interface CommandToDevice {}




    @Retention(RetentionPolicy.RUNTIME)
    public @interface CommandUpdateFromDevice {}


    @Retention(RetentionPolicy.RUNTIME)
    public @interface CommandUpdateToClient {}


    @Retention(RetentionPolicy.RUNTIME)
    public @interface NotificationFromDevice {}


    @Retention(RetentionPolicy.RUNTIME)
    public @interface NotificationToUser {}

    @Retention(RetentionPolicy.RUNTIME)
    public @interface NotificationToDevice {}



    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeviceSubmitted {}




    @Retention(RetentionPolicy.RUNTIME)
    public @interface DevicePublished {}



    @Retention(RetentionPolicy.RUNTIME)
    public @interface WebsocketField {}




    @Retention(RetentionPolicy.RUNTIME)
    public @interface Submitted {}
}
