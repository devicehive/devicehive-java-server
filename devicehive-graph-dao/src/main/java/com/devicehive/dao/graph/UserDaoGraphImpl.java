package com.devicehive.dao.graph;

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

import com.devicehive.dao.UserDao;
import com.devicehive.dao.graph.model.UserVertex;
import com.devicehive.vo.NetworkVO;
import com.devicehive.vo.UserVO;
import com.devicehive.vo.UserWithNetworkVO;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;

@Repository
public class UserDaoGraphImpl extends GraphGenericDao implements UserDao {

    private static final Logger logger = LoggerFactory.getLogger(UserDaoGraphImpl.class);

    @Override
    public Optional<UserVO> findByName(String name) {
        logger.info("Getting user by login");
        GraphTraversal<Vertex, Vertex> gT = g.V().has(UserVertex.LABEL, UserVertex.Properties.LOGIN, name)
                .not(g.V().has(UserVertex.LABEL, UserVertex.Properties.STATUS, 3));
        if (gT.hasNext()) {
            UserVO userVO = UserVertex.toVO(gT.next());
            return Optional.of(userVO);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public UserVO findByGoogleName(String name) {
        logger.info("Getting user by google login");
        GraphTraversal<Vertex, Vertex> gT = g.V().has(UserVertex.LABEL, UserVertex.Properties.GOOGLE_LOGIN, name.toLowerCase())
                .not(g.V().has(UserVertex.LABEL, UserVertex.Properties.STATUS, 3));
        if (gT.hasNext()) {
            return UserVertex.toVO(gT.next());
        } else {
            return null;
        }
    }

    @Override
    public UserVO findByFacebookName(String name) {
        logger.info("Getting user by facebook login");
        GraphTraversal<Vertex, Vertex> gT = g.V().has(UserVertex.LABEL, UserVertex.Properties.FACEBOOK_LOGIN, name.toLowerCase())
                .not(g.V().has(UserVertex.LABEL, UserVertex.Properties.STATUS, 3));
        if (gT.hasNext()) {
            return UserVertex.toVO(gT.next());
        } else {
            return null;
        }
    }

    @Override
    public UserVO findByGithubName(String name) {
        logger.info("Getting user by github login");
        GraphTraversal<Vertex, Vertex> gT = g.V().has(UserVertex.LABEL, UserVertex.Properties.GITHUB_LOGIN, name.toLowerCase())
                .not(g.V().has(UserVertex.LABEL, UserVertex.Properties.STATUS, 3));
        if (gT.hasNext()) {
            return UserVertex.toVO(gT.next());
        } else {
            return null;
        }
    }

    @Override
    public Optional<UserVO> findByIdentityName(String login, String googleLogin, String facebookLogin, String githubLogin) {
        logger.info("Getting user by identity login");
        GraphTraversal<Vertex, Vertex> gT = g.V().has(UserVertex.LABEL, UserVertex.Properties.LOGIN, login)
                .or(g.V().has(UserVertex.LABEL, UserVertex.Properties.GOOGLE_LOGIN, googleLogin.toLowerCase()),
                        g.V().has(UserVertex.LABEL, UserVertex.Properties.FACEBOOK_LOGIN, facebookLogin.toLowerCase()),
                        g.V().has(UserVertex.LABEL, UserVertex.Properties.GITHUB_LOGIN, githubLogin.toLowerCase()))
                .not(g.V().has(UserVertex.LABEL, UserVertex.Properties.STATUS, 3));
        if (gT.hasNext()) {
            UserVO userVO = UserVertex.toVO(gT.next());
            return Optional.of(userVO);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public long hasAccessToNetwork(UserVO user, NetworkVO network) {
        return 0;
    }

    @Override
    public long hasAccessToDevice(UserVO user, String deviceGuid) {
        return 0;
    }

    @Override
    public UserWithNetworkVO getWithNetworksById(long id) {
        return null;
    }

    @Override
    public int deleteById(long id) {
        GraphTraversal<Vertex, Vertex> gT = g.V().has(UserVertex.LABEL, UserVertex.Properties.ID, id);
        int count = gT.asAdmin()
                .clone()
                .toList()
                .size();

        gT.drop();
        executeStatement(gT);
        return count;
    }

    @Override
    public UserVO find(Long id) {
        GraphTraversal<Vertex, Vertex> gT = g.V().has(UserVertex.LABEL, UserVertex.Properties.ID, id);
        if (gT.hasNext()) {
            return UserVertex.toVO(gT.next());
        } else {
            return null;
        }
    }

    @Override
    public void persist(UserVO user) {
        logger.info("Adding new user");
        GraphTraversal<Vertex, Vertex> gT = UserVertex.toVertex(user, g);

        executeStatement(gT);
    }

    @Override
    public UserVO merge(UserVO existing) {
        return null;
    }

    @Override
    public void unassignNetwork(@NotNull UserVO existingUser, @NotNull long networkId) {

    }

    @Override
    public List<UserVO> list(String login, String loginPattern, Integer status, String sortField, Boolean sortOrderAsc, Integer take, Integer skip) {
        return null;
    }
}
