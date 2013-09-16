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

    public static InetAddress getClientIP(){
         return IP.get();
    }

    public static void setClientIP(InetAddress ip){
       IP.set(ip);
    }

    public static void setHostName(String hostName){
        HOST_NAME.set(hostName);
    }

    public static String getHostName(){
        return HOST_NAME.get();
    }

    public static void clean(){
        REQUEST = null;
        PRINCIPAL_KEEPER = null;
        SESSION = null;
        IP = null;
        HOST_NAME = null;
    }

}
