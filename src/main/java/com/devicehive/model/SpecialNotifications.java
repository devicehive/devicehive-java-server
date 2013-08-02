package com.devicehive.model;


public final class SpecialNotifications {
    /**
     * Server-originated notification about new device registration
     */
    public static final String DEVICE_ADD = "$device-add";
    /**
     * Server-originated notification about device changes
     */
    public static final String DEVICE_UPDATE = "$device-update";
    /**
     * Device originated notification about equipment state change
     */
    public static final String EQUIPMENT = "equipment";
    /**
     * Device originated notification about device status change
     */
    public static final String DEVICE_STATUS = "device-status";
}
