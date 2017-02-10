package com.devicehive.dao.graph.model;

/*
 * #%L
 * DeviceHive Dao RDBMS Implementation
 * %%
 * Copyright (C) 2016 - 2017 DataArt
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

import com.devicehive.model.JsonStringWrapper;
import com.devicehive.model.enums.UserStatus;
import com.devicehive.vo.UserVO;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.util.Date;

public class UserVertex {

    public static final String LABEL = "User";

    public static UserVO toVO(Vertex v) {
        UserVO vo = new UserVO();
        vo.setId(Long.parseLong((String) v.property(Properties.ID).value()));
        vo.setData(new JsonStringWrapper(v.property(Properties.DATA).isPresent() ? (String) v.property(Properties.DATA).value() : null));
        vo.setFacebookLogin(v.property(Properties.FACEBOOK_LOGIN).isPresent() ? (String) v.property(Properties.FACEBOOK_LOGIN).value() : null);
        vo.setGithubLogin(v.property(Properties.GITHUB_LOGIN).isPresent() ? (String) v.property(Properties.GITHUB_LOGIN).value() : null);
        vo.setGoogleLogin(v.property(Properties.GOOGLE_LOGIN).isPresent() ? (String) v.property(Properties.GOOGLE_LOGIN).value() : null);
        vo.setLastLogin(v.property(Properties.LAST_LOGIN).isPresent() ? new Date((long) v.property(Properties.LAST_LOGIN).value()) : null);
        vo.setLogin(v.property(Properties.LOGIN).isPresent() ? (String) v.property(Properties.LOGIN).value() : null);
        vo.setLoginAttempts(v.property(Properties.LOGIN_ATTEMPTS).isPresent() ? Integer.parseInt((String) v.property(Properties.LOGIN_ATTEMPTS).value()) : null);
        vo.setPasswordHash(v.property(Properties.PASSWORD_HASH).isPresent() ? (String) v.property(Properties.PASSWORD_HASH).value() : null);
        vo.setPasswordSalt(v.property(Properties.PASSWORD_SALT).isPresent() ? (String) v.property(Properties.PASSWORD_SALT).value() : null);
        vo.setStatus(v.property(Properties.STATUS).isPresent() ? UserStatus.getValueForIndex(Integer.parseInt((String) v.property(Properties.STATUS).value())) : null);
        return vo;
    }

    public static GraphTraversal<Vertex, Vertex> toVertex(UserVO vo, GraphTraversalSource g) {
        return g.addV(UserVertex.LABEL)
                // FIXME: execution fails with null variables
                .property(UserVertex.Properties.ID, vo.getId())
                .property(UserVertex.Properties.DATA, vo.getData())
                .property(UserVertex.Properties.FACEBOOK_LOGIN, vo.getFacebookLogin() != null ? vo.getFacebookLogin().toLowerCase() : null)
                .property(UserVertex.Properties.GITHUB_LOGIN, vo.getGithubLogin() != null ? vo.getGithubLogin().toLowerCase() : null)
                .property(UserVertex.Properties.GOOGLE_LOGIN, vo.getGoogleLogin() != null ? vo.getGoogleLogin().toLowerCase() : null)
                .property(UserVertex.Properties.LAST_LOGIN, vo.getLastLogin())
                .property(UserVertex.Properties.LOGIN, vo.getLogin())
                .property(UserVertex.Properties.LOGIN_ATTEMPTS, vo.getLoginAttempts())
                .property(UserVertex.Properties.PASSWORD_HASH, vo.getPasswordHash())
                .property(UserVertex.Properties.PASSWORD_SALT, vo.getPasswordSalt())
                .property(UserVertex.Properties.STATUS, vo.getStatus());
    }

    public class Properties {
        public static final String ID = "id";
        public static final String DATA = "data";
        public static final String FACEBOOK_LOGIN = "facebook_login";
        public static final String GITHUB_LOGIN = "github_login";
        public static final String GOOGLE_LOGIN = "google_login";
        public static final String LAST_LOGIN = "last_login";
        public static final String LOGIN = "login";
        public static final String LOGIN_ATTEMPTS = "login_attempts";
        public static final String PASSWORD_HASH = "password_hash";
        public static final String PASSWORD_SALT = "password_salt";
        public static final String STATUS = "status";
    }
}
