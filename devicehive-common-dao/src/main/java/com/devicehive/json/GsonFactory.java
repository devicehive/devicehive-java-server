package com.devicehive.json;

/*
 * #%L
 * DeviceHive Common Dao Interfaces
 * %%
 * Copyright (C) 2016 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import com.devicehive.json.adapters.*;
import com.devicehive.json.strategies.AnnotatedStrategy;
import com.devicehive.model.enums.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy;

public class GsonFactory {

    private static Map<Policy, Gson> cache = new ConcurrentHashMap<>();
    private static Gson gson = createGsonBuilder().create();

    public static Gson createGson() {
        return gson;
    }

    public static Gson createGson(Policy policy) {
        Gson gson = cache.get(policy);
        if (gson != null) {
            return gson;
        }
        gson = createGsonBuilder()
            .addDeserializationExclusionStrategy(new AnnotatedStrategy(policy))
            .addSerializationExclusionStrategy(new AnnotatedStrategy(policy))
            .create();
        cache.put(policy, gson);
        return gson;
    }


    private static GsonBuilder createGsonBuilder() {
        return new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .serializeNulls()
            .registerTypeAdapterFactory(new OptionalAdapterFactory())
            .registerTypeAdapterFactory(new JsonStringWrapperAdapterFactory())
            .registerTypeAdapter(Date.class, new TimestampAdapter())
            .registerTypeAdapter(UserRole.class, new UserRoleAdapter())
            .registerTypeAdapter(UserStatus.class, new UserStatusAdapter())
            .registerTypeAdapter(Type.class, new OAuthTypeAdapter())
            .registerTypeAdapter(AccessType.class, new AccessTypeAdapter())
            .registerTypeAdapter(AccessKeyType.class, new AccessKeyStatusAdapter());
    }

}
