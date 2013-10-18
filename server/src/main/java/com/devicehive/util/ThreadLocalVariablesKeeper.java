package com.devicehive.util;


import com.devicehive.auth.HivePrincipal;
import com.devicehive.model.OAuthClient;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.Session;
import java.net.InetAddress;

public final class ThreadLocalVariablesKeeper {

    private static Logger logger = LoggerFactory.getLogger(ThreadLocalVariablesKeeper.class);
    private static ThreadLocal<JsonObject> REQUEST = new ThreadLocal<>();
    private static ThreadLocal<Session> SESSION = new ThreadLocal<>();
    private static ThreadLocal<HivePrincipal> PRINCIPAL_KEEPER = new ThreadLocal<>();
    private static ThreadLocal<InetAddress> IP = new ThreadLocal<>();
    private static ThreadLocal<String> HOST_NAME = new ThreadLocal<>();
    private static ThreadLocal<OAuthClient> OAUTH_CLIENT = new ThreadLocal<>();

    public static JsonObject getRequest() {
        return REQUEST.get();
    }

    public static void setRequest(JsonObject request) {
        REQUEST.set(request);
    }

    public static Session getSession() {
        return SESSION.get();
    }

    public static void setSession(Session session) {
        SESSION.set(session);
    }

    public static HivePrincipal getPrincipal() {
        logger.debug("GetPrincipal. ThreadName : {}. Principal : {}. Principal value : {}",
                Thread.currentThread().getName(), PRINCIPAL_KEEPER, PRINCIPAL_KEEPER.get());
        return PRINCIPAL_KEEPER.get();
    }

    public static void setPrincipal(HivePrincipal principal) {
        logger.debug("SetPrincipal : ThreadName : {}. Principal : {}. Principal current value : {}. Principal new " +
                "value : {}",
                Thread.currentThread().getName(), PRINCIPAL_KEEPER, PRINCIPAL_KEEPER.get(), principal);
        PRINCIPAL_KEEPER.set(principal);
        logger.info("SetPrincipal : ThreadName : {}. Principal : {}. Principal current value : {}.",
                Thread.currentThread().getName(), PRINCIPAL_KEEPER, PRINCIPAL_KEEPER.get());
    }

    public static InetAddress getClientIP() {
        return IP.get();
    }

    public static void setClientIP(InetAddress ip) {
        IP.set(ip);
    }

    public static String getHostName() {
        return HOST_NAME.get();
    }

    public static void setHostName(String hostName) {
        HOST_NAME.set(hostName);
    }

    public static OAuthClient getOAuthClient() {
        return OAUTH_CLIENT.get();
    }

    public static void setOAuthClient(OAuthClient client) {
        OAUTH_CLIENT.set(client);
    }

    public static void clean() {
        logger.debug("Clean : ThreadName : {}.", Thread.currentThread().getName());
        REQUEST.set(null);
        PRINCIPAL_KEEPER.set(null);
        SESSION.set(null);
        IP.set(null);
        HOST_NAME.set(null);
        OAUTH_CLIENT.set(null);
    }

}
