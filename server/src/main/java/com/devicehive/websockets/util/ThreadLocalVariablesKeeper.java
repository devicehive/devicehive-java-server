package com.devicehive.websockets.util;


import com.devicehive.auth.HivePrincipal;
import com.google.gson.JsonObject;

import javax.websocket.Session;

public final class ThreadLocalVariablesKeeper {

//    private static ThreadLocal<ImmutablePair<JsonObject, Session>> JSON_AND_SESSION_KEEPER = new ThreadLocal<>();
    private static ThreadLocal<JsonObject> REQUEST = new ThreadLocal<>();
    private static ThreadLocal<Session> SESSION = new ThreadLocal<>();
    private static ThreadLocal<HivePrincipal> PRINCIPAL_KEEPER = new ThreadLocal<>();

    public static JsonObject getRequest(){
        return REQUEST.get();
    }

    public static void setRequest(JsonObject request){
        REQUEST.set(request);
    }

    public static void setSession(Session session){
        SESSION.set(session);
    }

    public static Session getSession(){
        return SESSION.get();
    }

    public static HivePrincipal getPrincipal(){
        return PRINCIPAL_KEEPER.get();
    }

    public static void setPrincipal(HivePrincipal principal){
        PRINCIPAL_KEEPER.set(principal);
    }

    public static void clean(){
        REQUEST = null;
        PRINCIPAL_KEEPER = null;
        SESSION = null;
    }

}
