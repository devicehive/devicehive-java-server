package com.devicehive.auth;

/*
 * #%L
 * DeviceHive Java Server Common business logic
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

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import java.net.InetAddress;
import java.util.Collection;

public class HiveAuthentication extends PreAuthenticatedAuthenticationToken {
    private static final long serialVersionUID = 3994727745773385047L;
    
    private HivePrincipal hivePrincipal;

    public HiveAuthentication(Object aPrincipal, Collection<? extends GrantedAuthority> anAuthorities) {
        super(aPrincipal, null, anAuthorities);
    }

    public HiveAuthentication(Object aPrincipal) {
        super(aPrincipal, null);
    }

    public HivePrincipal getHivePrincipal() {
        return hivePrincipal;
    }

    public void setHivePrincipal(HivePrincipal hivePrincipal) {
        this.hivePrincipal = hivePrincipal;
    }

    public static class HiveAuthDetails {
        private InetAddress clientInetAddress;
        private String origin;
        private String authorization;

        public HiveAuthDetails(InetAddress clientInetAddress, String origin, String authorization) {
            this.clientInetAddress = clientInetAddress;
            this.origin = origin;
            this.authorization = authorization;
        }

        public InetAddress getClientInetAddress() {
            return clientInetAddress;
        }

        public String getOrigin() {
            return origin;
        }

        public String getAuthorization() {
            return authorization;
        }
    }
}
