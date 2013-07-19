package com.devicehive.websockets.json.strategies;


import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

import java.lang.annotation.Annotation;

public class AnnotatedStrategy implements ExclusionStrategy {

    private Class<? extends Annotation> annotationClass;

    public AnnotatedStrategy(Class<? extends Annotation> annotationClass) {
        this.annotationClass = annotationClass;
    }

    @Override
    public boolean shouldSkipField(FieldAttributes f) {
        return f.getAnnotation(annotationClass) == null;
    }

    @Override
    public boolean shouldSkipClass(Class<?> clazz) {
        return false;
    }
}
