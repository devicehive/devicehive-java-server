package com.devicehive.exceptions;

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

public class HivePersistenceLayerException extends HiveException {
    private static final long serialVersionUID = 2084328743134451009L;

    public HivePersistenceLayerException(String message, Throwable cause) {
        super(message, cause);
    }

    public HivePersistenceLayerException(String message) {
        super(message);
    }

    public HivePersistenceLayerException(String message, Integer code) {
        super(message, code);
    }

    public HivePersistenceLayerException(String message, Throwable cause, Integer code) {
        super(message, cause, code);
    }
}
