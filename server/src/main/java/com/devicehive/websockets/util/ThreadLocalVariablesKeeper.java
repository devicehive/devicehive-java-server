package com.devicehive.websockets.util;


import com.devicehive.auth.HivePrincipal;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.websocket.Session;

public final class ThreadLocalVariablesKeeper {

    private static final ThreadLocal<ImmutablePair<JsonObject, Session>> JSON_AND_SESSION_KEEPER = new ThreadLocal<>();
    private static final ThreadLocal<HivePrincipal> PRINCIPAL_KEEPER = new ThreadLocal<>();

    public static ImmutablePair<JsonObject, Session> getJsonAndSession(){
        return JSON_AND_SESSION_KEEPER.get();
    }

    public static void setJsonAndSession(ImmutablePair<JsonObject, Session> value){
        JSON_AND_SESSION_KEEPER.set(value);
    }

    public static HivePrincipal getPrincipal(){
        return PRINCIPAL_KEEPER.get();
    }

    public static void setPrincipal(HivePrincipal principal){
        PRINCIPAL_KEEPER.set(principal);
    }

}
