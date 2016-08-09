package com.devicehive.shim.api.client;

import java.util.Arrays;
import java.util.Objects;

public class Request {

    private final String contentType;
    private final byte[] body;
    private final String replyTo;
    private final String correlationId;

    private Request(String contentType, byte[] body, String replyTo, String correlationId) {
        this.contentType = contentType;
        this.body = body;
        this.replyTo = replyTo;
        this.correlationId = correlationId;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getBody() {
        return body;
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
        if (!(o instanceof Request)) return false;
        Request request = (Request) o;
        return Objects.equals(contentType, request.contentType) &&
                Arrays.equals(body, request.body) &&
                Objects.equals(replyTo, request.replyTo) &&
                Objects.equals(correlationId, request.correlationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contentType, body, replyTo, correlationId);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private String contentType;
        private byte[] body;
        private String replyTo;
        private String correlationId;

        public Builder withContentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder withBody(byte[] body) {
            this.body = body;
            return this;
        }

        public Builder withReplyTo(String replyTo) {
            this.replyTo = replyTo;
            return this;
        }

        public Builder withCorrelationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Request build() {
            return new Request(contentType, body, replyTo, correlationId);
        }

    }
}
