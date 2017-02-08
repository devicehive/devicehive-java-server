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

import com.devicehive.auth.HivePrincipal;
import com.devicehive.dao.DeviceDao;
import com.devicehive.vo.DeviceVO;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

@Repository
public class DeviceDaoGraphImpl implements DeviceDao {

    @Override
    public DeviceVO findByUUID(String uuid) {
        return null;
    }

    @Override
    public void persist(DeviceVO device) {

    }

    @Override
    public DeviceVO merge(DeviceVO device) {
        return null;
    }

    @Override
    public int deleteByUUID(String guid) {
        return 0;
    }

    @Override
    public List<DeviceVO> getDeviceList(List<String> guids, HivePrincipal principal) {
        return null;
    }

    @Override
    public long getAllowedDeviceCount(HivePrincipal principal, List<String> guids) {
        return 0;
    }

    @Override
    public List<DeviceVO> list(String name, String namePattern, String status, Long networkId, String networkName, Long deviceClassId, String deviceClassName, String sortField, @NotNull Boolean sortOrderAsc, Integer take, Integer skip, HivePrincipal principal) {
        return null;
    }

    @Override
    public Map<String, Integer> getOfflineTimeForDevices(List<String> guids) {
        return null;
    }

    @Override
    public void changeStatusForDevices(String status, List<String> guids) {

    }
}
