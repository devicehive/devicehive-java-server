package com.devicehive.shim.api;

/*
 * #%L
 * DeviceHive Shim  API Interfaces
 * %%
 * Copyright (C) 2016 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

@SuppressWarnings("unused")
public class Response {

    @SerializedName("b")
    private Body body;

    @SerializedName("cId")
    private String correlationId;

    @SerializedName("l")
    private boolean last;

    @SerializedName("err")
    private int errorCode;

    @SerializedName("fld")
    private boolean failed;

    private Response(Body body, String correlationId, boolean last, int errorCode, boolean failed) {
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

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
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
        private boolean last = true;
        private String correlationId;

        public Builder<T> withBody(T body) {
            this.body = body;
            return this;
        }

        public Builder<T> withCorrelationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder<T> withLast(boolean last) {
            this.last = last;
            return this;
        }

        public Response buildFailed(int errorCode) {
            return new Response(body, correlationId, last, errorCode, true);
        }

        public Response buildSuccess() {
            return new Response(body, correlationId, last, 0, false);
        }

    }
}
