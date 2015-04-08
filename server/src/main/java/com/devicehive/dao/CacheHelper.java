package com.devicehive.dao;

import javax.persistence.CacheRetrieveMode;
import javax.persistence.CacheStoreMode;
import javax.persistence.Query;

public class CacheHelper {

    private static final String CACHEBLE = "org.hibernate.cacheable";
    private static final String RETRIEVE_MODE = "javax.persistence.cache.retrieveMode";
    private static final String STORE_MODE = "javax.persistence.cache.storeMode";

    public static void cacheable(Query query) {
        query.setHint(CACHEBLE, true);
        query.setHint(RETRIEVE_MODE, CacheRetrieveMode.USE);
        query.setHint(STORE_MODE, CacheStoreMode.REFRESH);
    }

    public static void notCacheable(Query query) {
        query.setHint(RETRIEVE_MODE, CacheRetrieveMode.BYPASS);
        query.setHint(STORE_MODE, CacheStoreMode.BYPASS);
    }
}
