package com.devicehive.model.view;

import com.devicehive.exceptions.HiveException;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.HiveEntity;
import com.devicehive.model.NullableWrapper;
import com.devicehive.model.UserRole;
import com.devicehive.model.UserStatus;
import com.devicehive.model.domain.Network;
import com.devicehive.model.domain.User;
import org.apache.commons.lang3.ObjectUtils;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

public class UserView implements HiveEntity {

    private static final long serialVersionUID = -8980491502416082011L;
    @JsonPolicyDef({COMMAND_TO_CLIENT, USER_PUBLISHED, COMMAND_TO_DEVICE, USERS_LISTED, USER_SUBMITTED})
    private Long id;
    @JsonPolicyDef({USER_PUBLISHED, USER_SUBMITTED, USERS_LISTED})
    private NullableWrapper<String> login;
    @JsonPolicyDef(USER_SUBMITTED)
    private NullableWrapper<String> password;
    @JsonPolicyDef({USER_PUBLISHED,USER_SUBMITTED, USERS_LISTED})
    private NullableWrapper<Integer> role;
    @JsonPolicyDef({USER_PUBLISHED,USER_SUBMITTED, USERS_LISTED})
    private NullableWrapper<Integer> status;
    @JsonPolicyDef({USER_PUBLISHED, USERS_LISTED, USER_SUBMITTED})
    private Timestamp lastLogin;
    @JsonPolicyDef({USER_PUBLISHED})
    private Set<UserNetworkView> networks;

    public UserView() {
    }

    public UserView(User user) {
        convertFrom(user);
    }

    public Long getId() {

        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public NullableWrapper<String> getLogin() {
        return login;
    }

    public void setLogin(NullableWrapper<String> login) {
        this.login = login;
    }

    public NullableWrapper<String> getPassword() {
        return password;
    }

    public void setPassword(NullableWrapper<String> password) {
        this.password = password;
    }

    public NullableWrapper<Integer> getRole() {
        return role;
    }

    public void setRole(NullableWrapper<Integer> role) {
        this.role = role;
    }

    public NullableWrapper<Integer> getStatus() {
        return status;
    }

    public void setStatus(NullableWrapper<Integer> status) {
        this.status = status;
    }

    public Timestamp getLastLogin() {
        return ObjectUtils.cloneIfPossible(lastLogin);
    }

    public void setLastLogin(Timestamp lastLogin) {
        this.lastLogin = ObjectUtils.cloneIfPossible(lastLogin);
    }

    public Set<UserNetworkView> getNetworks() {
        return networks;
    }

    public void setNetworks(Set<UserNetworkView> networks) {
        this.networks = networks;
    }

    public User convertTo() {
        User user = new User();
        user.setId(id);
        user.setLastLogin(lastLogin);
        if (login != null) {
            user.setLogin(login.getValue());
        }
        if (role != null) {
            switch (role.getValue()) {
                case 0:
                    user.setRole(UserRole.ADMIN);
                    break;
                case 1:
                    user.setRole(UserRole.CLIENT);
                    break;
                default:
                    throw new HiveException("Unparseable user role : " + role);
            }
        }
        if (status != null) {
            switch (status.getValue()) {
                case 0:
                    user.setStatus(UserStatus.ACTIVE);
                    break;
                case 1:
                    user.setStatus(UserStatus.LOCKED_OUT);
                    break;
                case 2:
                    user.setStatus(UserStatus.DISABLED);
                    break;
                case 3:
                    user.setStatus(UserStatus.DELETED);
                    break;
                default:
                    throw new HiveException("Unparseable user status : " + status);
            }
        }
        if (networks != null) {
            Set<Network> networkSet = new HashSet<>(networks.size());
            for (UserNetworkView current : networks) {
                networkSet.add(current.convertToNetwork());
            }
            user.setNetworks(networkSet);
        }
        return user;
    }

    public void convertFrom(User user) {
        if (user == null) {
            return;
        }
        id = user.getId();
        login = new NullableWrapper<>(user.getLogin());
        role = new NullableWrapper<>(user.getRole().getValue());
        status = new NullableWrapper<>(user.getStatus().getValue());
        if (user.getNetworks() != null) {
            Set<UserNetworkView> networkViewSet = new HashSet<>(user.getNetworks().size());
            for (Network current : user.getNetworks()) {
                networkViewSet.add(new UserNetworkView(current));
            }
            networks = networkViewSet;
        }
    }
}
