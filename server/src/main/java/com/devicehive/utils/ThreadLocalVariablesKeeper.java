package com.devicehive.utils;


import com.devicehive.auth.HivePrincipal;
import com.google.gson.JsonObject;

import javax.websocket.Session;
import java.net.InetAddress;

public final class ThreadLocalVariablesKeeper {

    private static ThreadLocal<JsonObject> REQUEST = new ThreadLocal<>();
    private static ThreadLocal<Session> SESSION = new ThreadLocal<>();
    private static ThreadLocal<HivePrincipal> PRINCIPAL_KEEPER = new ThreadLocal<>();
    private static ThreadLocal<InetAddress> IP = new ThreadLocal<>();
    private static ThreadLocal<String> HOST_NAME = new ThreadLocal<>();

    public static JsonObject getRequest() {
        return REQUEST.get();
    }

    public static void setRequest(JsonObject request) {
        if (REQUEST == null) {
            REQUEST = new ThreadLocal<>();
        }
        REQUEST.set(request);
    }

    public static Session getSession() {
        if (SESSION == null){
            return null;
        }
        return SESSION.get();
    }

    public static void setSession(Session session) {
        if (SESSION == null) {
            SESSION = new ThreadLocal<>();
        }
        SESSION.set(session);
    }

    public static HivePrincipal getPrincipal() {
        if (PRINCIPAL_KEEPER == null){
            return null;
        }
        return PRINCIPAL_KEEPER.get();
    }

    public static void setPrincipal(HivePrincipal principal) {
        if (PRINCIPAL_KEEPER == null) {
            PRINCIPAL_KEEPER = new ThreadLocal<>();
        }
        PRINCIPAL_KEEPER.set(principal);
    }

    public static InetAddress getClientIP() {
        if (IP == null){
            return null;
        }
        return IP.get();
    }

    public static void setClientIP(InetAddress ip) {
        if (IP == null) {
            IP = new ThreadLocal<>();
        }
        IP.set(ip);
    }

    public static String getHostName() {
        if (HOST_NAME == null){
            return null;
        }
        return HOST_NAME.get();
    }

    public static void setHostName(String hostName) {
        if (HOST_NAME == null) {
            HOST_NAME = new ThreadLocal<>();
        }
        HOST_NAME.set(hostName);
    }

    public static void clean() {
        REQUEST = null;
        PRINCIPAL_KEEPER = null;
        SESSION = null;
        IP = null;
        HOST_NAME = null;
    }

}
