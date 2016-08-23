package com.devicehive.shim.api;

import java.util.Objects;

public class Response {

    private Body body;
    private String correlationId;
    private boolean last;

    private int errorCode;
    private boolean failed;

    public Response(Body body, String correlationId, boolean last, int errorCode, boolean failed) {
        this.body = body;
        this.correlationId = correlationId;
        this.last = last;
        this.errorCode = errorCode;
        this.failed = failed;
    }

    public Body getBody() {
        return body;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public boolean isLast() {
        return last;
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
        return last == response.last &&
                errorCode == response.errorCode &&
                failed == response.failed &&
                Objects.equals(body, response.body) &&
                Objects.equals(correlationId, response.correlationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(body, correlationId, last, errorCode, failed);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Response{");
        sb.append(", body=").append(body);
        sb.append(", correlationId='").append(correlationId).append('\'');
        sb.append(", last=").append(last);
        sb.append(", errorCode=").append(errorCode);
        sb.append(", failed=").append(failed);
        sb.append('}');
        return sb.toString();
    }

    public static <T extends Body> Builder<T> newBuilder() {
        return new Builder<> ();
    }

    public static class Builder<T extends Body> {
        private T body;
        private boolean last;
        private String correlationId;
        private int errorCode;

        public Builder withBody(T body) {
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

        public Builder withLast(boolean last) {
            this.last = last;
            return this;
        }

        public Response buildFailed() {
            return new Response(body, correlationId, last, errorCode, true);
        }

        public Response buildSuccess() {
            return new Response(body, correlationId, last, errorCode, false);
        }

    }
}
