package com.devicehive.model;


public class NullableWrapper<K> {

    private K value;

    public NullableWrapper(K value) {
        this.value = value;
    }

    public K getValue() {
        return value;
    }
}
