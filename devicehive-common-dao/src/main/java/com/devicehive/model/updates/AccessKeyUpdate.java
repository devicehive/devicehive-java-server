package com.devicehive.model.updates;

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

import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.HiveEntity;
import com.devicehive.model.enums.AccessKeyType;
import com.devicehive.vo.AccessKeyPermissionVO;
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
    private Optional<Set<AccessKeyPermissionVO>> permissions;

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


    public Optional<Set<AccessKeyPermissionVO>> getPermissions() {
        return permissions;
    }

    public void setPermissions(Optional<Set<AccessKeyPermissionVO>> permissions) {
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
