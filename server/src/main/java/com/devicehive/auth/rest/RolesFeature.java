package com.devicehive.auth.rest;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;
import java.lang.reflect.Method;
import java.util.Arrays;

@Provider
public class RolesFeature implements DynamicFeature {

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        Method method = resourceInfo.getResourceMethod();
        if (method.isAnnotationPresent(DenyAll.class)) {
            context.register(new DenyAllFilter());
            return;
        } else if (method.isAnnotationPresent(RolesAllowed.class)) {
            RolesAllowed rolesAllowed = method.getAnnotation(RolesAllowed.class);
            context.register(new RolesAllowedFilter(Arrays.asList(rolesAllowed.value())));
            return;
        } else if (method.isAnnotationPresent(PermitAll.class)) {
            return;
        }

        Class resourceClass = resourceInfo.getResourceClass();
        if (resourceClass.isAnnotationPresent(DenyAll.class)) {
            context.register(new DenyAllFilter());
            return;
        } else if (resourceClass.isAnnotationPresent(RolesAllowed.class)) {
            RolesAllowed rolesAllowed = (RolesAllowed) resourceClass.getAnnotation(RolesAllowed.class);
            context.register(new RolesAllowedFilter(Arrays.asList(rolesAllowed.value())));
            return;
        } else if (method.isAnnotationPresent(PermitAll.class)) {
            return;
        }
        context.register(new DenyAllFilter());
    }
}
