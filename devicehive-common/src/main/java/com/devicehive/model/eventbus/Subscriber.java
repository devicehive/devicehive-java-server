package com.devicehive.model.eventbus;

import java.util.Objects;

public class Subscriber {

    private String id;
    private String replyTo;
    private String correlationId;

    public Subscriber(String id, String replyTo, String correlationId) {
        this.id = id;
        this.replyTo = replyTo;
        this.correlationId = correlationId;
    }

    public String getId() {
        return id;
    }

    public String getReplyTo() {
        return replyTo;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Subscriber)) return false;
        Subscriber that = (Subscriber) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, replyTo, correlationId);
    }

    @Override
    public String toString() {
        return "Subscriber{" +
                "id='" + id + '\'' +
                ", replyTo='" + replyTo + '\'' +
                ", correlationId='" + correlationId + '\'' +
                '}';
    }
}
