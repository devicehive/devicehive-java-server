package com.devicehive.model;


import java.io.Serializable;

public class NullableWrapper<K> implements Serializable {

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
