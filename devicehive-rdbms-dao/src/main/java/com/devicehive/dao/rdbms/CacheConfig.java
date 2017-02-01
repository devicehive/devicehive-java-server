package com.devicehive.dao.rdbms;

/*
 * #%L
 * DeviceHive Dao RDBMS Implementation
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

import javax.persistence.CacheRetrieveMode;
import javax.persistence.CacheStoreMode;

public class CacheConfig {

    private CacheRetrieveMode retrieveMode;
    private CacheStoreMode storeMode;

    private CacheConfig(CacheRetrieveMode retrieveMode, CacheStoreMode storeMode) {
        this.retrieveMode = retrieveMode;
        this.storeMode = storeMode;
    }

    public CacheRetrieveMode getRetrieveMode() {
        return retrieveMode;
    }

    public CacheStoreMode getStoreMode() {
        return storeMode;
    }

    /**
     * get entities from cache
     */
    public static CacheConfig get() {
        return new CacheConfig(CacheRetrieveMode.USE, CacheStoreMode.USE);
    }

    /**
     * get entities from db and refresh cache
     */
    public static CacheConfig refresh() {
        return new CacheConfig(CacheRetrieveMode.USE, CacheStoreMode.REFRESH);
    }

    /**
     * bypass cache
     */
    public static CacheConfig bypass() {
        return new CacheConfig(CacheRetrieveMode.BYPASS, CacheStoreMode.BYPASS);
    }
}
