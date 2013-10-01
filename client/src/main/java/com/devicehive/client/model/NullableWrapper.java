package com.devicehive.client.model;

import java.io.Serializable;

public class NullableWrapper<K> implements Serializable {

    private static final long serialVersionUID = 5760788287985397290L;
    private K value;

    public NullableWrapper(K value) {
        this.value = value;
    }

    public NullableWrapper() {
    }

    public K getValue() {
        return value;
    }

    public void setValue(K value) {
        this.value = value;
    }
}
