package com.devicehive.controller.converters;

import com.devicehive.model.HiveEntity;

import javax.ws.rs.ext.Provider;
import java.util.Collection;

@Provider
public class CollectionProvider extends JsonPolicyProvider<Collection<? extends HiveEntity>> {

}
