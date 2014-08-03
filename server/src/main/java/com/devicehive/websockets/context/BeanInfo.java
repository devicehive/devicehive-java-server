package com.devicehive.websockets.context;

import javax.enterprise.context.spi.CreationalContext;

class BeanInfo<T> {
        private CreationalContext<T> creationalContext;
        private T instance;

        BeanInfo(CreationalContext<T> creationalContext, T instance) {
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
