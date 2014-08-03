package com.devicehive.websockets.context;

import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.websocket.Session;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;


public class WebsocketMessageContext implements Context {
    private static final Logger logger = LoggerFactory.getLogger(WebsocketContextExtension.class);

    private final ThreadLocal<Map<Contextual, BeanInfo>> beanStore = new ThreadLocal<>();

    private final ThreadLocal<JsonObject> requestThreadLocal = new ThreadLocal<>();

    private final ThreadLocal<Session> sessionThreadLocal = new ThreadLocal<>();

    public WebsocketMessageContext() {
        logger.info("WebsocketMessageContext is created.");
    }



    @Override
    public Class<? extends Annotation> getScope() {
        return RequestScoped.class;
    }

    @Override
    public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
        checkActive();
        T instance = contextual.create(creationalContext);
        BeanInfo<T> info = new BeanInfo<>(creationalContext, instance);
        beanStore.get().put(contextual, info);
        return instance;
    }

    @Override
    public <T> T get(Contextual<T> contextual) {
        checkActive();
        @SuppressWarnings("unchecked")
        BeanInfo<T> info = beanStore.get().get(contextual);
        return info != null ? info.getInstance() : null;
    }

    @Override
    public boolean isActive() {
        return requestThreadLocal.get() != null;
    }

    public void activate(JsonObject request, Session session) {
        requestThreadLocal.set(request);
        sessionThreadLocal.set(session);
        beanStore.set(new HashMap<Contextual, BeanInfo>());
    }

    public void deactivate() {
        requestThreadLocal.remove();
        for (Map.Entry<Contextual, BeanInfo> entry : beanStore.get().entrySet()) {
            entry.getKey().destroy(entry.getValue().getInstance(), entry.getValue().getCreationalContext());
        }
        requestThreadLocal.remove();
        sessionThreadLocal.remove();
        beanStore.remove();
    }


    private void checkActive() {
        if (!isActive()) {
            throw new ContextNotActiveException("WebsocketMessageContext is not active");
        }
    }


}
