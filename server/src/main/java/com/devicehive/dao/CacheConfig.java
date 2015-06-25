package com.devicehive.dao;

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
