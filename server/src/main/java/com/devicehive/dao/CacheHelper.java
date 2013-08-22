package com.devicehive.dao;

import javax.persistence.Query;

public class CacheHelper {

    private static final String CACHE_HINT = "org.hibernate.cacheable";

    public static void cacheable(Query query) {
        query.setHint(CACHE_HINT, true);
    }

    public static void notCacheable(Query query) {
        query.setHint(CACHE_HINT, false);
    }
}
