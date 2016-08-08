package com.devicehive.vo;

import com.devicehive.json.strategies.JsonPolicyDef;

import java.util.HashSet;
import java.util.Set;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.USER_PUBLISHED;

public class UserWithNetworkVO extends UserVO {

    @JsonPolicyDef({USER_PUBLISHED})
    private Set<NetworkVO> networks;

    public Set<NetworkVO> getNetworks() {
        return networks;
    }

    public void setNetworks(Set<NetworkVO> networks) {
        this.networks = networks;
    }

    public static UserWithNetworkVO fromUserVO(UserVO dc) {
        UserWithNetworkVO vo = null;
        if (dc != null) {
            vo = new UserWithNetworkVO();
            vo.setData(dc.getData());
            vo.setFacebookLogin(dc.getFacebookLogin());
            vo.setGithubLogin(dc.getGithubLogin());
            vo.setGoogleLogin(dc.getGoogleLogin());
            vo.setId(dc.getId());
            vo.setLastLogin(dc.getLastLogin());
            vo.setLogin(dc.getLogin());
            vo.setLoginAttempts(dc.getLoginAttempts());
            vo.setNetworks(new HashSet<>());
            vo.setPasswordHash(dc.getPasswordHash());
            vo.setPasswordSalt(dc.getPasswordSalt());
            vo.setRole(dc.getRole());
            vo.setStatus(dc.getStatus());
        }

        return vo;
    }

}
