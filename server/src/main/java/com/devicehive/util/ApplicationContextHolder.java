package com.devicehive.util;

import org.springframework.context.ApplicationContext;

public class ApplicationContextHolder {
    private static ApplicationContextHolder ourInstance = new ApplicationContextHolder();

    public static ApplicationContextHolder getInstance() {
        return ourInstance;
    }

    private ApplicationContextHolder() {
    }

    private ApplicationContext context;

    public ApplicationContext get() {
        if (context == null) {
            throw new IllegalStateException("Application context in null");
        }
        return context;
    }

    public void set(ApplicationContext context) {
        this.context = context;
    }
}
