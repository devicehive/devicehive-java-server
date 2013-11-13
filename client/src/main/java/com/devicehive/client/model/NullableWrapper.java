package com.devicehive.client.model;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

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

    public static <K> K value(NullableWrapper<K> wrapper) {
        return wrapper != null ? wrapper.getValue() : null;
    }

    public static <K> NullableWrapper<K> create(K value) {
        return new NullableWrapper<>(value);
    }

    @Override
    public String toString() {
        return ObjectUtils.toString(value, null);
    }
}
