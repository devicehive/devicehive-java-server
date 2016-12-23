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
import javax.persistence.Query;

public class CacheHelper {

    public static final String CACHEBLE = "org.hibernate.cacheable";
    public static final String RETRIEVE_MODE = "javax.persistence.cache.retrieveMode";
    public static final String STORE_MODE = "javax.persistence.cache.storeMode";

    public static void cacheable(Query query) {
        query.setHint(CACHEBLE, true);
        query.setHint(RETRIEVE_MODE, CacheRetrieveMode.USE);
        query.setHint(STORE_MODE, CacheStoreMode.USE);
    }
}
