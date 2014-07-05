package com.devicehive.client.impl.util;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

public class LockWrapper implements AutoCloseable {

    private final Lock lock;

    private LockWrapper(Lock lock) {
        this.lock = lock;
        lock.lock();
    }

    public static LockWrapper using(Lock lock) {
        return new LockWrapper(lock);
    }

    public static LockWrapper read(ReadWriteLock lock) {
        return new LockWrapper(lock.readLock());
    }

    public static LockWrapper write(ReadWriteLock lock) {
        return new LockWrapper(lock.writeLock());
    }

    @Override
    public void close() {
        this.lock.unlock();
    }
}
