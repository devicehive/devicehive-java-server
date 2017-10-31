package com.devicehive.dao;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.vo.DeviceTypeVO;
import com.devicehive.vo.DeviceTypeWithUsersAndDevicesVO;
import com.devicehive.vo.UserVO;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface DeviceTypeDao {
    List<DeviceTypeVO> findByName(String name);

    void persist(DeviceTypeVO newDeviceType);

    List<DeviceTypeWithUsersAndDevicesVO> getDeviceTypesByIdsAndUsers(Long idForFiltering, Set<Long> singleton, Set<Long> permittedDeviceTypes);

    int deleteById(long id);

    DeviceTypeVO find(@NotNull Long deviceTypeId);

    DeviceTypeVO merge(DeviceTypeVO existing);

    void assignToDeviceType(DeviceTypeVO deviceType, UserVO user);

    List<DeviceTypeVO> list(String name, String namePattern, String sortField, boolean sortOrderAsc, Integer take,
                         Integer skip, Optional<HivePrincipal> principal);

    Optional<DeviceTypeVO> findFirstByName(String name);

    Optional<DeviceTypeWithUsersAndDevicesVO> findWithUsers(@NotNull long deviceTypeId);

    Optional<DeviceTypeVO> findDefaultByUser(long userId);
}
