package com.devicehive.dao;

import com.devicehive.vo.AccessKeyPermissionVO;
import com.devicehive.vo.AccessKeyVO;
import com.devicehive.vo.UserVO;

import java.util.Date;
import java.util.List;
import java.util.Optional;

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
