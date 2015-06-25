package com.devicehive.model;


import java.io.Serializable;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NullableWrapper)) return false;
        NullableWrapper<?> that = (NullableWrapper<?>) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
