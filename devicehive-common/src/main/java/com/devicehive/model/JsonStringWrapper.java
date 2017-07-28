package com.devicehive.model;

/*
 * #%L
 * DeviceHive Common Dao Interfaces
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

import com.devicehive.configuration.Messages;
import com.devicehive.exceptions.HiveException;
import com.google.gson.JsonParser;

import javax.persistence.Embeddable;
import java.util.Objects;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

@Embeddable
public class JsonStringWrapper implements HiveEntity {

    private static final long serialVersionUID = -152849186108390497L;
    private String jsonString;

    public JsonStringWrapper() {
    }

    public JsonStringWrapper(String jsonString) {
        setJsonString(jsonString);
    }

    public String getJsonString() {
        return jsonString;
    }

    public void setJsonString(String jsonString) {
        try {
            new JsonParser().parse(jsonString).getAsJsonObject();
        } catch (Exception e) {
            throw new HiveException(Messages.PARAMS_NOT_JSON, BAD_REQUEST.getStatusCode());
        }
        this.jsonString = jsonString;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JsonStringWrapper)) return false;
        JsonStringWrapper that = (JsonStringWrapper) o;
        return Objects.equals(jsonString, that.jsonString);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jsonString);
    }

    @Override
    public String toString() {
        return "JsonStringWrapper{" +
                "jsonString='" + jsonString + '\'' +
                '}';
    }
}
