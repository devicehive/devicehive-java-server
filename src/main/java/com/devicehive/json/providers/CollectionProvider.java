package com.devicehive.json.providers;

import java.util.Collection;

import javax.ws.rs.ext.Provider;

import com.devicehive.model.HiveEntity;

@Provider
public class CollectionProvider extends JsonPolicyProvider<Collection<? extends HiveEntity>> {

}
