package com.devicehive.dao.riak.model;

/*
 * #%L
 * DeviceHive Dao Riak Implementation
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

import com.devicehive.vo.IdentityProviderVO;

public class RiakIdentityProvider {
    private static final long serialVersionUID = 1959997436981843212L;

    private String name;

    private String apiEndpoint;

    private String verificationEndpoint;

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

        RiakIdentityProvider user = (RiakIdentityProvider) o;

        return name != null && name.equals(user.name);
    }

    @Override
    public int hashCode() {
        return name == null ? 0 : name.hashCode();
    }

    public static IdentityProviderVO convertToVO(RiakIdentityProvider identityProvider) {
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

    public static RiakIdentityProvider convertToEntity(IdentityProviderVO identityProvider) {
        RiakIdentityProvider vo = null;
        if (identityProvider != null) {
            vo = new RiakIdentityProvider();
            vo.setApiEndpoint(identityProvider.getApiEndpoint());
            vo.setName(identityProvider.getName());
            vo.setTokenEndpoint(identityProvider.getTokenEndpoint());
            vo.setVerificationEndpoint(identityProvider.getVerificationEndpoint());
        }
        return vo;
    }
}
