package com.devicehive.dao;

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

import com.devicehive.vo.OAuthClientVO;

import java.util.List;

public interface OAuthClientDao {
    int deleteById(Long id);

    OAuthClientVO getByOAuthId(String oauthId);

    OAuthClientVO getByName(String name);

    OAuthClientVO getByOAuthIdAndSecret(String id, String secret);

    OAuthClientVO find(Long id);

    void persist(OAuthClientVO oAuthClient);

    OAuthClientVO merge(OAuthClientVO existing);

    List<OAuthClientVO> get(String name,
                          String namePattern,
                          String domain,
                          String oauthId,
                          String sortField,
                          Boolean sortOrderAsc,
                          Integer take,
                          Integer skip);
}
