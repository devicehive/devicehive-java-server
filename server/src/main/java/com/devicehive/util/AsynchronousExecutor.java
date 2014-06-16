package com.devicehive.util;

import javax.annotation.Resource;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

@Stateless
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class AsynchronousExecutor {

    @Resource
    private ManagedExecutorService managedExecutorService;


    public Future<?> execute(@NotNull Runnable runnable) {
        return managedExecutorService.submit(runnable);
    }

    public <V> Future<V> execute(Callable<V> callable) throws Exception {
        return managedExecutorService.submit(callable);
    }
}
