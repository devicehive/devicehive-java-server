package com.devicehive.websockets.json.strategies.client;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

/**
 * Created with IntelliJ IDEA.
 * User: jkulagina
 * Date: 18.06.13
 * Time: 20:59
 */
public class ServerInfoResponseExclusionStrategy implements ExclusionStrategy {

    @Override
    public boolean shouldSkipField(FieldAttributes fieldAttributes) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean shouldSkipClass(Class<?> aClass) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
