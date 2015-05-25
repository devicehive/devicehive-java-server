package com.devicehive.controller.converters;

import com.devicehive.model.HiveEntity;

import java.util.Collection;

import javax.ws.rs.ext.Provider;

@Provider
public class CollectionProvider extends JsonPolicyProvider<Collection<? extends HiveEntity>> {

}
