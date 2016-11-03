package com.devicehive.vo;

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

import com.devicehive.exceptions.HiveException;
import com.devicehive.json.GsonFactory;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.HiveEntity;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.model.Subnet;
import com.google.gson.*;

import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

public class AccessKeyPermissionVO implements HiveEntity {

    private Long id;

    @JsonPolicyDef({ACCESS_KEY_LISTED, ACCESS_KEY_PUBLISHED, OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED})
    private JsonStringWrapper domains;

    @JsonPolicyDef({ACCESS_KEY_LISTED, ACCESS_KEY_PUBLISHED, OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED})
    private JsonStringWrapper subnets;

    @JsonPolicyDef({ACCESS_KEY_LISTED, ACCESS_KEY_PUBLISHED, OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED})
    private JsonStringWrapper actions;

    @JsonPolicyDef({ACCESS_KEY_LISTED, ACCESS_KEY_PUBLISHED, OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED})
    private JsonStringWrapper networkIds;

    @JsonPolicyDef({ACCESS_KEY_LISTED, ACCESS_KEY_PUBLISHED, OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED})
    private JsonStringWrapper deviceGuids;

    private long entityVersion;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public JsonStringWrapper getDomains() {
        return domains;
    }

    public void setDomains(JsonStringWrapper domains) {
        this.domains = domains;
    }

    public JsonStringWrapper getSubnets() {
        return subnets;
    }

    public void setSubnets(JsonStringWrapper subnets) {
        this.subnets = subnets;
    }

    public JsonStringWrapper getActions() {
        return actions;
    }

    public void setActions(JsonStringWrapper actions) {
        this.actions = actions;
    }

    public JsonStringWrapper getNetworkIds() {
        return networkIds;
    }

    public void setNetworkIds(JsonStringWrapper networkIds) {
        this.networkIds = networkIds;
    }

    public JsonStringWrapper getDeviceGuids() {
        return deviceGuids;
    }

    public void setDeviceGuids(JsonStringWrapper deviceGuids) {
        this.deviceGuids = deviceGuids;
    }

    public long getEntityVersion() {
        return entityVersion;
    }

    public void setEntityVersion(long entityVersion) {
        this.entityVersion = entityVersion;
    }
    //// Some nasty helpers.

    public void setActionsArray(String... actions) {
        Gson gson = GsonFactory.createGson();
        this.actions = new JsonStringWrapper(gson.toJsonTree(actions).toString());
    }

    public Set<String> getDomainsAsSet() {
        return getJsonAsSet(domains);
    }

    public Set<Subnet> getSubnetsAsSet() {
        if (subnets == null) {
            return null;
        }
        JsonParser parser = new JsonParser();
        JsonElement elem = parser.parse(subnets.getJsonString());
        if (elem instanceof JsonNull) {
            return null;
        }
        if (elem instanceof JsonArray) {
            JsonArray json = (JsonArray) elem;
            Set<Subnet> result = new HashSet<>(json.size());
            for (JsonElement current : json) {
                if (!current.isJsonNull()) {
                    result.add(new Subnet(current.getAsString()));
                } else {
                    result.add(null);
                }
            }
            return result;
        }
        throw new HiveException("JSON array expected!", HttpServletResponse.SC_BAD_REQUEST);
    }

    public Set<String> getActionsAsSet() {
        return getJsonAsSet(actions);
    }

    public Set<String> getDeviceGuidsAsSet() {
        return getJsonAsSet(deviceGuids);
    }

    public Set<Long> getNetworkIdsAsSet() {
        if (networkIds == null) {
            return null;
        }
        JsonParser parser = new JsonParser();
        JsonElement elem = parser.parse(networkIds.getJsonString());
        if (elem instanceof JsonNull) {
            return null;
        }
        if (elem instanceof JsonArray) {
            JsonArray json = (JsonArray) elem;
            Set<Long> result = new HashSet<>(json.size());
            for (JsonElement current : json) {
                result.add(current.getAsLong());
            }
            return result;
        }
        throw new HiveException("JSON array expected!", HttpServletResponse.SC_BAD_REQUEST);
    }

    public void setDomainArray(String... domains) {
        Gson gson = GsonFactory.createGson();
        this.domains = new JsonStringWrapper(gson.toJsonTree(domains).toString());
    }

    public void setSubnetsArray(String... subnets) {
        Gson gson = GsonFactory.createGson();
        this.subnets = new JsonStringWrapper(gson.toJsonTree(subnets).toString());
    }

    public void setNetworkIdsCollection(Collection<Long> actions) {
        Gson gson = GsonFactory.createGson();
        this.networkIds = new JsonStringWrapper(gson.toJsonTree(actions).toString());
    }

    public void setDeviceGuidsCollection(Collection<String> deviceGuids) {
        Gson gson = GsonFactory.createGson();
        this.deviceGuids = new JsonStringWrapper(gson.toJsonTree(deviceGuids).toString());
    }

    private Set<String> getJsonAsSet(JsonStringWrapper wrapper) {
        if (wrapper == null) {
            return null;
        }
        JsonParser parser = new JsonParser();
        JsonElement elem = parser.parse(wrapper.getJsonString());
        if (elem instanceof JsonNull) {
            return null;
        }

        if (elem instanceof JsonArray) {
            JsonArray json = (JsonArray) elem;
            Set<String> result = new HashSet<>(json.size());
            for (JsonElement current : json) {
                result.add(current.getAsString());
            }
            return result;
        }
        throw new HiveException("JSON array expected!", HttpServletResponse.SC_BAD_REQUEST);
    }


}
