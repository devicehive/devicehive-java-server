package com.devicehive.shim.api;

import java.util.Objects;

public abstract class Body {

    protected String action;

    protected Body(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }

    /**
     * Method aimed to simplify casting constructions to concrete body implementations
     * @param clazz class to cast to
     * @return original body object casted to concrete implementation class
     */
    public <T extends Body> T cast(Class<T> clazz) {
        return clazz.cast(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Body)) return false;
        Body body = (Body) o;
        return Objects.equals(action, body.action);
    }

    @Override
    public int hashCode() {
        return Objects.hash(action);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Body{");
        sb.append("action='").append(action).append('\'');
        sb.append('}');
        return sb.toString();
    }

}
