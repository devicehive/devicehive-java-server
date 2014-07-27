package com.devicehive.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.spi.AlterableContext;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


public class HiveRequestContext implements AlterableContext {
    private static final Logger logger = LoggerFactory.getLogger(HiveContextExtension.class);

    private final ThreadLocal<UUID> contextId = new ThreadLocal<>();

    private final ConcurrentMap<UUID, ConcurrentMap<Contextual, HiveInstanceInfo>> instances = new ConcurrentHashMap<>();

    public HiveRequestContext() {
        logger.info("HiveRequestContext is created.");
    }



    @Override
    public Class<? extends Annotation> getScope() {
        return HiveRequestScoped.class;
    }

    @Override
    public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
        checkActive();
        T instance = contextual.create(creationalContext);
        HiveInstanceInfo<T> info = new HiveInstanceInfo<>(creationalContext, instance);
        instances.get(contextId.get()).put(contextual, info);
        return instance;
    }

    @Override
    public <T> T get(Contextual<T> contextual) {
        checkActive();
        @SuppressWarnings("unchecked")
        HiveInstanceInfo<T> info = instances.get(contextId.get()).get(contextual);
        if (info == null) {
            return null;
        }
        return info.instance;
    }

    @Override
    public boolean isActive() {
        return contextId.get() != null;
    }

    public void activate() {
        if (isActive()) {
            logger.warn("HiveRequestContext is already activated");
            return;
        }
        logger.info("Activating HiveRequestContext...");
        UUID id = UUID.randomUUID();
        instances.put(id, new ConcurrentHashMap<Contextual, HiveInstanceInfo>());
        contextId.set(id);
        //TODO @Initialized event?
        logger.info("HiveRequestContext is activated.");
    }

    public void deactivate() {
        UUID id = contextId.get();
        if (!isActive()) {
            logger.warn("HiveRequestContext is already deactivated");
            return;
        }
        logger.info("Deactivating HiveRequestContext...");
        contextId.remove();
        ConcurrentMap<Contextual, HiveInstanceInfo> map = instances.remove(id);
        for (Map.Entry<Contextual, HiveInstanceInfo> entry : map.entrySet()) {
            entry.getKey().destroy(entry.getValue().getInstance(), entry.getValue().getCreationalContext());
        }
        //TODO @Destroyed event?
        logger.info("HiveRequestContext is deactivated.");
    }

    @Override
    public void destroy(Contextual contextual) {
        checkActive();
        HiveInstanceInfo info = instances.get(contextId.get()).get(contextual);
        contextual.destroy(info.getInstance(), info.getCreationalContext());
    }

    private void checkActive() {
        if (!isActive()) {
            throw new ContextNotActiveException("HiveRequestContext is not active");
        }
    }


    private static class HiveInstanceInfo<T> {
        private CreationalContext<T> creationalContext;
        private T instance;

        private HiveInstanceInfo(CreationalContext<T> creationalContext, T instance) {
            this.creationalContext = creationalContext;
            this.instance = instance;
        }

        public CreationalContext<T> getCreationalContext() {
            return creationalContext;
        }

        public T getInstance() {
            return instance;
        }
    }
}
