package com.devicehive.model.updates;


import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.AccessKey;
import com.devicehive.model.AccessKeyPermission;
import com.devicehive.model.HiveEntity;
import com.devicehive.model.NullableWrapper;

import java.sql.Timestamp;
import java.util.Set;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.ACCESS_KEY_PUBLISHED;

public class AccessKeyUpdate implements HiveEntity{

    private static final long serialVersionUID = -979668798467393194L;

    @JsonPolicyDef(ACCESS_KEY_PUBLISHED)
    private NullableWrapper<String> label;

    @JsonPolicyDef(ACCESS_KEY_PUBLISHED)
    private NullableWrapper<Timestamp> expirationDate;

    @JsonPolicyDef(ACCESS_KEY_PUBLISHED)
    private NullableWrapper<Set<AccessKeyPermission>> permissions;


    public NullableWrapper<String> getLabel() {
        return label;
    }

    public void setLabel(NullableWrapper<String> label) {
        this.label = label;
    }

    public NullableWrapper<Timestamp> getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(NullableWrapper<Timestamp> expirationDate) {
        this.expirationDate = expirationDate;
    }


    public NullableWrapper<Set<AccessKeyPermission>> getPermissions() {
        return permissions;
    }

    public void setPermissions(NullableWrapper<Set<AccessKeyPermission>> permissions) {
        this.permissions = permissions;
    }

    public AccessKey convertTo(){
      AccessKey result = new AccessKey();
        if (label != null){
            result.setLabel(label.getValue());
        }
        if (expirationDate != null){
            result.setExpirationDate(expirationDate.getValue());
        }
        if (permissions != null){

            result.setPermissions(permissions.getValue());
        }
        return result;
    }
}
