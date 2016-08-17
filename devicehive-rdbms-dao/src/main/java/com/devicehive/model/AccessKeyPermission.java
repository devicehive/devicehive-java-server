package com.devicehive.model;

/*
 * #%L
 * DeviceHive Dao RDBMS Implementation
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
import com.devicehive.vo.AccessKeyPermissionVO;
import com.devicehive.vo.AccessKeyVO;
import com.google.gson.*;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

@Entity
@NamedQueries({
        @NamedQuery(name = "AccessKeyPermission.deleteByAccessKey", query = "delete from AccessKeyPermission akp where akp.accessKey = :accessKey")
})
@Table(name = "access_key_permission")
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
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

    public void setDeviceGuidsCollection(Collection<String> deviceGuids) {
        Gson gson = GsonFactory.createGson();
        this.deviceGuids = new JsonStringWrapper(gson.toJsonTree(deviceGuids).toString());
    }

    public static AccessKeyPermission convert(AccessKeyPermissionVO accessKey) {
        AccessKeyPermission result = null;
        if (accessKey != null) {
            result = new AccessKeyPermission();
            result.setId(accessKey.getId());
            result.setActions(accessKey.getActions());
            result.setDeviceGuids(accessKey.getDeviceGuids());
            result.setDomains(accessKey.getDomains());
            result.setNetworkIds(accessKey.getNetworkIds());
            result.setSubnets(accessKey.getSubnets());
        }
        return result;
    }

    public static AccessKeyPermissionVO convert(AccessKeyPermission accessKey) {
        AccessKeyPermissionVO result = null;
        if (accessKey != null) {
            result = new AccessKeyPermissionVO();
            result.setId(accessKey.getId());
            result.setActions(accessKey.getActions());
            result.setDeviceGuids(accessKey.getDeviceGuids());
            result.setDomains(accessKey.getDomains());
            result.setNetworkIds(accessKey.getNetworkIds());
            result.setSubnets(accessKey.getSubnets());
        }
        return result;
    }

    public static Set<AccessKeyPermission> convertToEntity(Collection<AccessKeyPermissionVO> accessKeys) {
        Set<AccessKeyPermission> result = null;
        if (accessKeys != null) {
            result = accessKeys.stream().map(AccessKeyPermission::convert).collect(Collectors.toSet());
        } else {
            result = Collections.emptySet();
        }
        return result;
    }

    public static Set<AccessKeyPermissionVO> converttoVO(Collection<AccessKeyPermission> accessKeys) {
        Set<AccessKeyPermissionVO> result = null;
        if (accessKeys != null) {
            result = accessKeys.stream().map(AccessKeyPermission::convert).collect(Collectors.toSet());
        } else {
            result = Collections.emptySet();
        }
        return result;
    }
}
