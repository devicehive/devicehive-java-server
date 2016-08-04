package com.devicehive.model.updates;

import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.AccessKey;
import com.devicehive.model.AccessKeyPermission;
import com.devicehive.model.HiveEntity;
import com.devicehive.model.enums.AccessKeyType;
import com.devicehive.vo.AccessKeyVO;

import java.util.Date;
import java.util.Optional;
import java.util.Set;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.ACCESS_KEY_PUBLISHED;

public class AccessKeyUpdate implements HiveEntity {

    private static final long serialVersionUID = -979668798467393194L;

    @JsonPolicyDef(ACCESS_KEY_PUBLISHED)
    private Optional<String> label;

    @JsonPolicyDef(ACCESS_KEY_PUBLISHED)
    private Optional<Date> expirationDate;

    @JsonPolicyDef(ACCESS_KEY_PUBLISHED)
    private Optional<Set<AccessKeyPermission>> permissions;

    @JsonPolicyDef(ACCESS_KEY_PUBLISHED)
    private Optional<Integer> type;


    public Optional<String> getLabel() {
        return label;
    }

    public void setLabel(Optional<String> label) {
        this.label = label;
    }

    public Optional<Date> getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Optional<Date> expirationDate) {
        this.expirationDate = expirationDate;
    }


    public Optional<Set<AccessKeyPermission>> getPermissions() {
        return permissions;
    }

    public void setPermissions(Optional<Set<AccessKeyPermission>> permissions) {
        this.permissions = permissions;
    }

    public Optional<Integer> getType() {
        return type;
    }

    public void setType(Optional<Integer> type) {
        this.type = type;
    }

    public AccessKeyType getTypeEnum() {
        if(type != null) {
            return type.map(AccessKeyType::getValueForIndex).orElse(null);
        }
        return null;
    }

    public AccessKeyVO convertTo() {
        AccessKeyVO result = new AccessKeyVO();

        if (label != null) {
            result.setLabel(label.orElse(null));
        }

        if (expirationDate != null) {
            result.setExpirationDate(expirationDate.orElse(null));
        }

        if (permissions != null) {
            result.setPermissions(permissions.orElse(null));
        }
        return result;
    }
}
