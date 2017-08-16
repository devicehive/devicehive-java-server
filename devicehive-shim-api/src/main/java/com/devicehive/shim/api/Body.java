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

public abstract class Body {

    @SerializedName("a")
    protected int action;

    protected Body(Action action) {
        this.action = action.ordinal();
    }

    public Action getAction() {
        return Action.values()[action];
    }

    /**
     * Method aimed to simplify casting constructions to concrete body implementations
     * @param clazz class to cast to
     * @return original body object casted to concrete implementation class
     */
    public <T extends Body> T cast(Class<T> clazz) {
        return clazz.cast(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Body)) return false;
        Body body = (Body) o;
        return Objects.equals(action, body.action);
    }

    @Override
    public int hashCode() {
        return Objects.hash(action);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Body{");
        sb.append("action='").append(action).append('\'');
        sb.append('}');
        return sb.toString();
    }

}
