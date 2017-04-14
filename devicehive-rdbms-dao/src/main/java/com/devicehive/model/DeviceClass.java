package com.devicehive.model;

/*
 * #%L
 * DeviceHive Dao RDBMS Implementation
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
import com.devicehive.vo.DeviceClassVO;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

/**
 * Represents a device class which holds meta-information about devices.
 */
@Entity
@Table(name = "device_class")
@NamedQueries({
        @NamedQuery(name = "DeviceClass.findByName", query = "select d from DeviceClass d where d.name = :name")
})
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class DeviceClass implements HiveEntity {

    public static final String NAME_COLUMN = "name";
    private static final long serialVersionUID = 8091624406245592117L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonPolicyDef(
            {DEVICE_PUBLISHED, NETWORK_PUBLISHED, DEVICECLASS_LISTED,
                    DEVICECLASS_PUBLISHED, DEVICECLASS_SUBMITTED})
    private Long id;

    @Column
    @NotNull(message = "name field cannot be null.")
    @Size(min = 1, max = 128, message = "Field cannot be empty. The length of name should not be more than 128 " +
            "symbols.")
    @JsonPolicyDef(
            {DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED, DEVICECLASS_LISTED,
                    DEVICECLASS_PUBLISHED})
    private String name;

    @Column(name = "is_permanent")
    @JsonPolicyDef(
        {DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED, DEVICECLASS_LISTED,
         DEVICECLASS_PUBLISHED})
    private Boolean isPermanent;

    @Embedded
    @AttributeOverrides({
                            @AttributeOverride(name = "jsonString", column = @Column(name = "data"))
                        })
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED, DEVICECLASS_LISTED, DEVICECLASS_PUBLISHED})
    private JsonStringWrapper data;

    @Version
    @Column(name = "entity_version")
    private Long entityVersion;

    public Long getEntityVersion() {
        return entityVersion;
    }

    public void setEntityVersion(Long entityVersion) {
        this.entityVersion = entityVersion;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getPermanent() {
        return isPermanent;
    }

    public void setPermanent(Boolean permanent) {
        isPermanent = permanent;
    }

    public JsonStringWrapper getData() {
        return data;
    }

    public void setData(JsonStringWrapper data) {
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DeviceClass that = (DeviceClass) o;

        return !(id != null ? !id.equals(that.id) : that.id != null);

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    public static DeviceClassVO convertToVo(DeviceClass deviceClass) {
        DeviceClassVO vo = null;
        if (deviceClass != null) {
            vo = new DeviceClassVO();
            vo.setName(deviceClass.getName());
            vo.setData(deviceClass.getData());
            vo.setId(deviceClass.getId());
            vo.setIsPermanent(deviceClass.getPermanent());
            vo.setEntityVersion(deviceClass.getEntityVersion());
        }
        return vo;
    }

    public static DeviceClass convertToEntity(DeviceClassVO vo) {
        DeviceClass en = null;
        if (vo != null) {
            en = new DeviceClass();
            en.setId(vo.getId());
            en.setData(vo.getData());
            en.setName(vo.getName());
            en.setPermanent(vo.getIsPermanent());
            en.setEntityVersion(vo.getEntityVersion());
        }
        return en;
    }
}
