package com.devicehive.vo;

import com.devicehive.json.strategies.JsonPolicyDef;

import java.util.HashSet;
import java.util.Set;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.USER_PUBLISHED;

public class UserWithDeviceTypeVO extends UserVO {

    @JsonPolicyDef({USER_PUBLISHED})
    private Set<DeviceTypeVO> deviceTypes;

    public Set<DeviceTypeVO> getDeviceTypes() {
        return deviceTypes;
    }

    public void setDeviceTypes(Set<DeviceTypeVO> deviceTypes) {
        this.deviceTypes = deviceTypes;
    }

    public static UserWithDeviceTypeVO fromUserVO(UserVO dc) {
        UserWithDeviceTypeVO vo = null;
        if (dc != null) {
            vo = new UserWithDeviceTypeVO();
            vo.setData(dc.getData());
            vo.setId(dc.getId());
            vo.setData(dc.getData());
            vo.setLastLogin(dc.getLastLogin());
            vo.setLogin(dc.getLogin());
            vo.setLoginAttempts(dc.getLoginAttempts());
            vo.setDeviceTypes(new HashSet<>());
            vo.setPasswordHash(dc.getPasswordHash());
            vo.setPasswordSalt(dc.getPasswordSalt());
            vo.setRole(dc.getRole());
            vo.setStatus(dc.getStatus());
            vo.setIntroReviewed(dc.getIntroReviewed());
        }

        return vo;
    }

}
