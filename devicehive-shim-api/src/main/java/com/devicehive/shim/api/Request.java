package com.devicehive.shim.api;

import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

public class Request {

    private Body body;
    private String correlationId;
    private String partitionKey;

    private boolean singleReplyExpected;

    private String replyTo;

    private Request(Body body,
                    boolean singleReplyExpected,
                    String correlationId,
                    String partitionKey) {
        this.body = body;
        this.singleReplyExpected = singleReplyExpected;
        this.correlationId = correlationId;
        this.partitionKey = partitionKey;
    }

    public Body getBody() {
        return body;
    }

    public String getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public boolean isSingleReplyExpected() {
        return singleReplyExpected;
    }

    public String getPartitionKey() {
        return partitionKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Request)) return false;
        Request request = (Request) o;
        return singleReplyExpected == request.singleReplyExpected &&
                Objects.equals(body, request.body) &&
                Objects.equals(correlationId, request.correlationId) &&
                Objects.equals(partitionKey, request.partitionKey) &&
                Objects.equals(replyTo, request.replyTo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(body, correlationId, singleReplyExpected, replyTo);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Request{");
        sb.append(", body=").append(body);
        sb.append(", correlationId='").append(correlationId).append('\'');
        sb.append(", singleReplyExpected=").append(singleReplyExpected);
        sb.append(", partitionKey=").append(partitionKey);
        sb.append(", replyTo='").append(replyTo).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public static <T extends Body> Builder<T> newBuilder() {
        return new Builder<>();
    }

    public static class Builder<T extends Body> {
        private T body;
        private String correlationId;
        private boolean singleReply = true;
        private String partitionKey;

        public Builder<T> withBody(T body) {
            this.body = body;
            return this;
        }

        public Builder<T> withCorrelationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder<T> withSingleReply(boolean singleReply) {
            this.singleReply = singleReply;
            return this;
        }

        public Builder<T> withPartitionKey(String key) {
            this.partitionKey = key;
            return this;
        }

        public Request build() {
            return new Request(
                    body, singleReply,
                    correlationId,
                    StringUtils.isBlank(partitionKey) // partitionKey is optional, set value to correlationId if it's blank
                            ? correlationId
                            : partitionKey
            );
        }

    }

}
