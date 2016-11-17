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

import com.devicehive.vo.AccessKeyPermissionVO;
import com.devicehive.vo.AccessKeyVO;
import com.devicehive.vo.UserVO;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Deprecated
public interface AccessKeyDao {
    AccessKeyVO getById(Long keyId, Long userId);

    Optional<AccessKeyVO> getByKey(String key);

    Optional<AccessKeyVO> getByUserAndLabel(UserVO user, String label);

    int deleteByIdAndUser(Long keyId, Long userId);

    int deleteById(Long keyId);

    int deleteOlderThan(Date date);

    AccessKeyVO find(Long id);

    void persist(AccessKeyVO accessKey);

    AccessKeyVO merge(AccessKeyVO existing);

    List<AccessKeyVO> list(Long userId, String label,
                           String labelPattern, Integer type,
                           String sortField, Boolean sortOrderAsc,
                           Integer take, Integer skip);

    int deleteByAccessKey(AccessKeyVO key);

    void persist(AccessKeyVO key, AccessKeyPermissionVO accessKeyPermission);

    AccessKeyPermissionVO merge(AccessKeyVO key, AccessKeyPermissionVO existing);

}
