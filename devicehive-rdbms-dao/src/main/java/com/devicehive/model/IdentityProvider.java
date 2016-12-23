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

import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.vo.IdentityProviderVO;
import com.google.gson.annotations.SerializedName;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.IDENTITY_PROVIDER_LISTED;

/**
 * Created by tmatvienko on 11/17/14.
 */
@Entity
@Table(name = "identity_provider")
@NamedQueries({
        @NamedQuery(name = "IdentityProvider.getByName", query = "select ip from IdentityProvider ip where ip.name = :name"),
        @NamedQuery(name = "IdentityProvider.deleteByName", query = "delete from IdentityProvider ip where ip.name = :name")
})
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class IdentityProvider implements HiveEntity {

    private static final long serialVersionUID = 1959997436981843212L;

    @Id
    @NotNull(message = "name field cannot be null.")
    @Size(min = 1, max = 64, message = "Field cannot be empty. The length of login should not be more than 64 " +
            "symbols.")
    @SerializedName("name")
    @JsonPolicyDef({IDENTITY_PROVIDER_LISTED})
    private String name;

    @Column(name = "api_endpoint")
    @NotNull(message = "identity provider's api endpoint can't be null.")
    @SerializedName("apiEndpoint")
    @JsonPolicyDef({IDENTITY_PROVIDER_LISTED})
    private String apiEndpoint;

    @Column(name = "verification_endpoint")
    @NotNull(message = "identity provider's verification endpoint can't be null.")
    @SerializedName("verificationEndpoint")
    @JsonPolicyDef({IDENTITY_PROVIDER_LISTED})
    private String verificationEndpoint;

    @Column(name = "token_endpoint")
    @NotNull(message = "identity provider's access token endpoint can't be null.")
    @SerializedName("tokenEndpoint")
    @JsonPolicyDef({IDENTITY_PROVIDER_LISTED})
    private String tokenEndpoint;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getApiEndpoint() {
        return apiEndpoint;
    }

    public void setApiEndpoint(String apiEndpoint) {
        this.apiEndpoint = apiEndpoint;
    }

    public String getVerificationEndpoint() {
        return verificationEndpoint;
    }

    public void setVerificationEndpoint(String verificationEndpoint) {
        this.verificationEndpoint = verificationEndpoint;
    }

    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    public void setTokenEndpoint(String tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        IdentityProvider user = (IdentityProvider) o;

        return name != null && name.equals(user.name);
    }

    @Override
    public int hashCode() {
        return name == null ? 0 : name.hashCode();
    }

    public static IdentityProviderVO convertToVO(IdentityProvider identityProvider) {
        IdentityProviderVO vo = null;
        if (identityProvider != null) {
            vo = new IdentityProviderVO();
            vo.setApiEndpoint(identityProvider.getApiEndpoint());
            vo.setName(identityProvider.getName());
            vo.setTokenEndpoint(identityProvider.getTokenEndpoint());
            vo.setVerificationEndpoint(identityProvider.getVerificationEndpoint());
        }
        return vo;
    }

    public static IdentityProvider convertToEntity(IdentityProviderVO identityProvider) {
        IdentityProvider vo = null;
        if (identityProvider != null) {
            vo = new IdentityProvider();
            vo.setApiEndpoint(identityProvider.getApiEndpoint());
            vo.setName(identityProvider.getName());
            vo.setTokenEndpoint(identityProvider.getTokenEndpoint());
            vo.setVerificationEndpoint(identityProvider.getVerificationEndpoint());
        }
        return vo;
    }
}
