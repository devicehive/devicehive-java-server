package com.devicehive.model;

import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "access_key_permission")
public class AccessKeyPermission implements HiveEntity {

    private static final long serialVersionUID = 728578066176830685L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "access_key_id")
    @NotNull
    private AccessKey accessKey;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "jsonString", column = @Column(name = "domains"))
    })
    @JsonPolicyApply(JsonPolicyDef.Policy.ACCESS_KEY_LISTED)
    private JsonStringWrapper domains;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "jsonString", column = @Column(name = "subnets"))
    })
    @JsonPolicyApply(JsonPolicyDef.Policy.ACCESS_KEY_LISTED)
    private JsonStringWrapper subnets;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "jsonString", column = @Column(name = "actions"))
    })
    @JsonPolicyApply(JsonPolicyDef.Policy.ACCESS_KEY_LISTED)
    private JsonStringWrapper actions;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "jsonString", column = @Column(name = "network_ids"))
    })
    @JsonPolicyApply(JsonPolicyDef.Policy.ACCESS_KEY_LISTED)
    private JsonStringWrapper networkIds;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "jsonString", column = @Column(name = "device_guids"))
    })
    @JsonPolicyApply(JsonPolicyDef.Policy.ACCESS_KEY_LISTED)
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

    public void setDomains(JsonStringWrapper domains) {
        this.domains = domains;
    }

    public Set<String> getDomainsAsSet() {
        return getJsonAsSet(domains);
    }

    public Set<String> getSubnetsAsSet(){
        return getJsonAsSet(subnets);
    }

    public Set<String> getActionsAsSet(){
        return getJsonAsSet(actions);
    }

    public Set<String> getDeviceGuidsAsSet(){
        return getJsonAsSet(deviceGuids);
    }

    public Set<Long> getNetworkIdsAsSet(){
        JsonParser parser = new JsonParser();
        JsonArray json = (JsonArray) parser.parse(networkIds.getJsonString());
        Set<Long> result = new HashSet<>(json.size());
        for (JsonElement current : json){
            result.add(current.getAsLong());
        }
        return result;
    }

    private Set<String> getJsonAsSet(JsonStringWrapper wrapper){
        JsonParser parser = new JsonParser();
        JsonArray json = (JsonArray) parser.parse(wrapper.getJsonString());
        Set<String> result = new HashSet<>(json.size());
        for (JsonElement current : json){
            result.add(current.getAsString());
        }
        return result;
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
}
