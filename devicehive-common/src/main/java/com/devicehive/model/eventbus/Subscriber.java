package com.devicehive.model.eventbus;

import java.util.Objects;

public class Subscriber {

    private String replyTo;
    private String id;

    public Subscriber(String replyTo, String id) {
        this.replyTo = replyTo;
        this.id = id;
    }

    public String getReplyTo() {
        return replyTo;
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Subscriber)) return false;
        Subscriber that = (Subscriber) o;
        return Objects.equals(replyTo, that.replyTo) &&
                Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(replyTo, id);
    }

    @Override
    public String toString() {
        return "Subscriber{" +
                "replyTo='" + replyTo + '\'' +
                ", id='" + id + '\'' +
                '}';
    }
}
