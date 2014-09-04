package com.devicehive.client.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Tool for differentiate entity that was not set and entity which were set null
 *
 * @param <K>
 */
public class NullableWrapper<K> implements Serializable {

    private static final long serialVersionUID = 5760788287985397290L;
    private K value;

    public NullableWrapper(K value) {
        this.value = value;
    }

    public NullableWrapper() {
    }

    public static <K> K value(NullableWrapper<K> wrapper) {
        return wrapper != null ? wrapper.getValue() : null;
    }

    public static <K> NullableWrapper<K> create(K value) {
        return new NullableWrapper<>(value);
    }

    public K getValue() {
        return value;
    }

    public void setValue(K value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return Objects.toString(value, null);
    }

}
