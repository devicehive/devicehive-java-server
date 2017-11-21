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

import com.hazelcast.nio.serialization.Portable;
import com.hazelcast.nio.serialization.PortableReader;
import com.hazelcast.nio.serialization.PortableWriter;

import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;

public class Subscriber implements Portable, Serializable {
    private static final long serialVersionUID = 9114135595987844152L;

    public static final int FACTORY_ID = 1;
    public static final int CLASS_ID = 6;

    private Long id;
    private String replyTo;
    private String correlationId;

    public Subscriber() {

    }

    public Subscriber(Long id, String replyTo, String correlationId) {
        this.id = id;
        this.replyTo = replyTo;
        this.correlationId = correlationId;
    }

    public Long getId() {
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

    @Override
    public int getFactoryId() {
        return FACTORY_ID;
    }

    @Override
    public int getClassId() {
        return CLASS_ID;
    }

    @Override
    public void writePortable(PortableWriter writer) throws IOException {
        writer.writeLong("id", id);
        writer.writeUTF("replyTo", replyTo);
        writer.writeUTF("correlationId", correlationId);
    }

    @Override
    public void readPortable(PortableReader reader) throws IOException {
        id = reader.readLong("id");
        replyTo = reader.readUTF("replyTo");
        correlationId = reader.readUTF("correlationId");
    }
}
