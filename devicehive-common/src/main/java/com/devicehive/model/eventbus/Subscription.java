package com.devicehive.model.eventbus;

import java.util.Objects;

/**
 * Event bus subscription object. Represents subscriptions for all types of events - notification, commands and command updates.
 */
public class Subscription {

    private String type;
    private String guid;
    private String name;

    /**
     * @param type - type of event to subscribe to (notification, command, command update)
     * @param guid - guid of event to subscribe to (id of device, command)
     */
    public Subscription(String type, String guid) {
        this.type = type;
        this.guid = guid;
    }

    /**
     * @param type - type of event to subscribe to (notification, command, command update)
     * @param guid - guid of event to subscribe to (id of device, command)
     * @param name - specific event name to subscribe to (notification name, command name)
     */
    public Subscription(String type, String guid, String name) {
        this.type = type;
        this.guid = guid;
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public String getGuid() {
        return guid;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Subscription)) return false;
        Subscription that = (Subscription) o;
        return Objects.equals(type, that.type) &&
                Objects.equals(guid, that.guid) &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, guid, name);
    }

    @Override
    public String toString() {
        return "Subscription{" +
                "type='" + type + '\'' +
                ", guid='" + guid + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
