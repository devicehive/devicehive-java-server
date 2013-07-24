package com.devicehive.model;

public interface Message extends HiveEntity {

    public Long getId();

    public Device getDevice();
}
