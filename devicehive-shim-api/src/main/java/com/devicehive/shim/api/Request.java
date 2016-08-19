package com.devicehive.shim.api;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Objects;

public class Request {

    private final String contentType;
    private final byte[] body;
    private final String correlationId;
    private final Action action;
    private final String partitionKey;

    private final boolean singleReplyExpected;

    private String replyTo;

    private Request(String contentType,
                    byte[] body,
                    boolean singleReplyExpected,
                    String correlationId,
                    Action action,
                    String partitionKey) {
        this.contentType = contentType;
        this.body = body;
        this.singleReplyExpected = singleReplyExpected;
        this.correlationId = correlationId;
        this.action = action;
        this.partitionKey = partitionKey;
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

    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public boolean isSingleReplyExpected() {
        return singleReplyExpected;
    }

    public Action getAction() {
        return action;
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
                Objects.equals(contentType, request.contentType) &&
                Arrays.equals(body, request.body) &&
                Objects.equals(correlationId, request.correlationId) &&
                Objects.equals(action, request.action) &&
                Objects.equals(partitionKey, request.partitionKey) &&
                Objects.equals(replyTo, request.replyTo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contentType, body, correlationId, singleReplyExpected, replyTo, action);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Request{");
        sb.append("contentType='").append(contentType).append('\'');
        sb.append(", body=").append(Arrays.toString(body));
        sb.append(", correlationId='").append(correlationId).append('\'');
        sb.append(", action='").append(action.toString()).append('\'');
        sb.append(", singleReplyExpected=").append(singleReplyExpected);
        sb.append(", partitionKey=").append(partitionKey);
        sb.append(", replyTo='").append(replyTo).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private String contentType;
        private byte[] body;
        private String correlationId;
        private boolean singleReply;
        private Action action;
        private String partitionKey;

        public Builder withContentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder withBody(byte[] body) {
            this.body = body;
            return this;
        }

        public Builder withCorrelationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder withSingleReply(boolean singleReply) {
            this.singleReply = singleReply;
            return this;
        }

        public Builder withAction(Action action) {
            this.action = action;
            return this;
        }

        public Builder withPartitionKey(String key) {
            this.partitionKey = key;
            return this;
        }

        public Request build() {
            return new Request(
                    contentType,
                    body, singleReply,
                    correlationId,
                    action,
                    StringUtils.isBlank(partitionKey) // partitionKey is mandatory, set value to correlationId if it's blank
                            ? correlationId
                            : partitionKey
            );
        }

    }

    public enum Action {
        NOTIFICATION_INSERT,
        NOTIFICATION_SUBSCRIBE,
        NOTIFICATION_UNSUBSCRIBE
    }
}
