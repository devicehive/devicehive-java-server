package com.devicehive.model.eventbus;

/*
 * #%L
 * DeviceHive Common Module
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
