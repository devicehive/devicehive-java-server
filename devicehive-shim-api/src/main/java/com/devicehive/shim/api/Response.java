package com.devicehive.shim.api;

import java.util.Arrays;
import java.util.Objects;

public class Response {

    private final String contentType;
    private final byte[] body;
    private final String correlationId;

    private final int errorCode;
    private final boolean failed;

    public Response(String contentType, byte[] body, String correlationId, int errorCode, boolean failed) {
        this.contentType = contentType;
        this.body = body;
        this.correlationId = correlationId;
        this.errorCode = errorCode;
        this.failed = failed;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getBody() {
        return body;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public boolean isFailed() {
        return failed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Response)) return false;
        Response response = (Response) o;
        return errorCode == response.errorCode &&
                failed == response.failed &&
                Objects.equals(contentType, response.contentType) &&
                Arrays.equals(body, response.body) &&
                Objects.equals(correlationId, response.correlationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contentType, body, correlationId, errorCode, failed);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Response{");
        sb.append("contentType='").append(contentType).append('\'');
        sb.append(", body=").append(Arrays.toString(body));
        sb.append(", correlationId='").append(correlationId).append('\'');
        sb.append(", errorCode=").append(errorCode);
        sb.append(", failed=").append(failed);
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
        private int errorCode;

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

        public Builder withErrorCode(int errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public Response buildFailed() {
            return new Response(contentType, body, correlationId, errorCode, true);
        }

        public Response buildSuccess() {
            return new Response(contentType, body, correlationId, errorCode, false);
        }

    }
}
