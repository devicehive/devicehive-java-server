package com.devicehive.model.enums;

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


import com.devicehive.exceptions.HiveException;

import javax.servlet.http.HttpServletResponse;

public enum Type {
    CODE("Code"),
    TOKEN("Token"),
    PASSWORD("Password");
    private final String value;

    Type(String value) {
        this.value = value;
    }

    public static Type forName(String value) {
        for (Type type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new HiveException("Illegal argument: " + value, HttpServletResponse.SC_BAD_REQUEST);

    }

    public String getValue() {
        return value;
    }

    public static Type getValueForIndex(int index){
        return values()[index];
    }

    @Override
    public String toString() {
        return this.value;
    }
}
