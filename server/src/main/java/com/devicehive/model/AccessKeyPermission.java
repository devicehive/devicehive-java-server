package com.devicehive.model;

import com.devicehive.exceptions.HiveException;
import com.devicehive.json.GsonFactory;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.google.gson.*;

import javax.persistence.*;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

@Entity
@NamedQueries({
        @NamedQuery(name = "AccessKeyPermission.deleteByAccessKey",
                query = "delete from AccessKeyPermission akp where akp.accessKey = :accessKey")
})
@Table(name = "access_key_permission")
public class AccessKeyPermission implements HiveEntity {

    private static final long serialVersionUID = 728578066176830685L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "access_key_id")
    @NotNull
    private AccessKey accessKey;
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "jsonString", column = @Column(name = "domains"))
    })
    @JsonPolicyDef({ACCESS_KEY_LISTED, ACCESS_KEY_PUBLISHED, OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED})
    private JsonStringWrapper domains;
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "jsonString", column = @Column(name = "subnets"))
    })
    @JsonPolicyDef({ACCESS_KEY_LISTED, ACCESS_KEY_PUBLISHED, OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED})
    private JsonStringWrapper subnets;
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "jsonString", column = @Column(name = "actions"))
    })
    @JsonPolicyDef({ACCESS_KEY_LISTED, ACCESS_KEY_PUBLISHED, OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED})
    private JsonStringWrapper actions;
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "jsonString", column = @Column(name = "network_ids"))
    })
    @JsonPolicyDef({ACCESS_KEY_LISTED, ACCESS_KEY_PUBLISHED, OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED})
    private JsonStringWrapper networkIds;
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "jsonString", column = @Column(name = "device_guids"))
    })
    @JsonPolicyDef({ACCESS_KEY_LISTED, ACCESS_KEY_PUBLISHED, OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED})
    private JsonStringWrapper deviceGuids;
    @Version
    @Column(name = "entity_version")
    private long entityVersion;

    public long getEntityVersion() {
        return entityVersion;
    }

    public void setEntityVersion(long entityVersion) {
        this.entityVersion = entityVersion;
    }

    public Long getId() {

        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AccessKey getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(AccessKey accessKey) {
        this.accessKey = accessKey;
    }

    public JsonStringWrapper getDomains() {
        return domains;
    }

    public void setDomains(String... domains) {
        Gson gson = GsonFactory.createGson();
        this.domains = new JsonStringWrapper(gson.toJsonTree(domains).toString());
    }

    public void setDomains(JsonStringWrapper domains) {
        this.domains = domains;
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
//            if (result.isEmpty()) {
//                result = null;
//            }
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

    public JsonStringWrapper getSubnets() {
        return subnets;
    }

    public void setSubnets(JsonStringWrapper subnets) {
        this.subnets = subnets;
    }

    public void setSubnets(String... subnets) {
        Gson gson = GsonFactory.createGson();
        this.subnets = new JsonStringWrapper(gson.toJsonTree(subnets).toString());
    }

    public JsonStringWrapper getActions() {
        return actions;
    }

    public void setActions(JsonStringWrapper actions) {
        this.actions = actions;
    }

    public void setActions(String... actions) {
        Gson gson = GsonFactory.createGson();
        this.actions = new JsonStringWrapper(gson.toJsonTree(actions).toString());
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
}
