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
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.UUID;

import static com.devicehive.shim.api.RequestType.clientRequest;

public class Request {

    @SerializedName("b")
    private Body body;

    @SerializedName("cId")
    private String correlationId;

    @SerializedName("pK")
    private String partitionKey;

    @SerializedName("sre")
    private boolean singleReplyExpected;

    @SerializedName("rTo")
    private String replyTo;

    @SerializedName("t")
    private int type;

    private Request(Body body,
                    boolean singleReplyExpected,
                    String correlationId,
                    String partitionKey) {
        this.body = body;
        this.singleReplyExpected = singleReplyExpected;
        this.correlationId = correlationId;
        this.partitionKey = partitionKey;
        this.type = clientRequest.ordinal();
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

    public void setType(RequestType type) {
        this.type = type.ordinal();
    }

    public RequestType getType() {
        return RequestType.values()[type];
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
        private String correlationId = UUID.randomUUID().toString();
        private boolean singleReply = true;
        private String partitionKey;

        public Builder<T> withBody(T body) {
            this.body = body;
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
