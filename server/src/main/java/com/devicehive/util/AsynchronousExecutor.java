package com.devicehive.util;

import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Local;
import javax.ejb.Stateless;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

@Stateless
@Local
public class AsynchronousExecutor {

    @Asynchronous
    public void execute(Runnable runnable) {
        runnable.run();
    }

    @Asynchronous
    public <V> Future<V> execute(Callable<V> callable) throws Exception {
        return new AsyncResult<V>(callable.call());
    }
}
