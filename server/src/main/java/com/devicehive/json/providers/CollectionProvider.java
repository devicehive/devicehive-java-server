package com.devicehive.json.providers;

import com.devicehive.model.HiveEntity;

import javax.ws.rs.ext.Provider;
import java.util.Collection;

@Provider
public class CollectionProvider extends JsonPolicyProvider<Collection<? extends HiveEntity>> {

}
