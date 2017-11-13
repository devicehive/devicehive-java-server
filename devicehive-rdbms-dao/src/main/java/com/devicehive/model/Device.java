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
import com.devicehive.vo.DeviceTypeVO;
import com.devicehive.vo.DeviceVO;
import com.devicehive.vo.NetworkVO;
import com.google.gson.annotations.SerializedName;
import io.swagger.annotations.ApiModelProperty;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

/**
 * TODO JavaDoc
 */


@Entity
@Table(name = "device")
@NamedQueries({
                  @NamedQuery(name = "Device.findById", query = "select d from Device d " +
                                                                  "left join fetch d.network " +
                                                                  "left join fetch d.deviceType " +
                                                                  "where d.deviceId = :deviceId"),
                  @NamedQuery(name = "Device.deleteById", query = "delete from Device d where d.deviceId = :deviceId")
              })
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Device implements HiveEntity {

    public static final String NETWORK_COLUMN = "network";
    public static final String DEVICE_ID_COLUMN = "device_id";

    private static final long serialVersionUID = 2959997451631843298L;

    @Id
    @SerializedName("sid") // overwork for  "declares multiple JSON fields" exception
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @SerializedName("id")
    @Column(name = "device_id")
    @NotNull(message = "id field cannot be null.")
    @Size(min = 1, max = 48, message = "Field cannot be empty. The length of guid should not be more than 48 symbols.")
    @JsonPolicyDef({DEVICE_PUBLISHED, NETWORK_PUBLISHED, DEVICE_TYPE_PUBLISHED})
    private String deviceId;

    @SerializedName("name")
    @Column
    @NotNull(message = "name field cannot be null.")
    @Size(min = 1, max = 128, message = "Field cannot be empty. The length of name should not be more than 128 " +
                                        "symbols.")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED, DEVICE_TYPE_PUBLISHED})
    private String name;

    @SerializedName("data")
    @Embedded
    @AttributeOverrides({
                            @AttributeOverride(name = "jsonString", column = @Column(name = "data"))
                        })
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED, DEVICE_TYPE_PUBLISHED})
    private JsonStringWrapper data;

    @SerializedName("networkId")
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "network_id")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED})
    private Network network;

    @SerializedName("deviceTypeId")
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "device_type_id")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED})
    private DeviceType deviceType;

    @Column(name = "blocked")
    @SerializedName("isBlocked")
    @ApiModelProperty(name="isBlocked")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED, DEVICE_TYPE_PUBLISHED})
    private Boolean blocked;

    public JsonStringWrapper getData() {
        return data;
    }

    public void setData(JsonStringWrapper data) {
        this.data = data;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Network getNetwork() {
        return network;
    }

    public void setNetwork(Network network) {
        this.network = network;
    }

    public DeviceType getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(DeviceType deviceType) {
        this.deviceType = deviceType;
    }

    public Boolean getBlocked() {
        return blocked;
    }

    public void setBlocked(Boolean blocked) {
        this.blocked = blocked;
    }

    public static class Queries {

        public interface Parameters {

            String DEVICE_ID = "deviceId";
            String ID = "id";
        }
    }

    public static DeviceVO convertToVo(Device dc) {
        DeviceVO vo = null;
        if (dc != null) {
            vo = new DeviceVO();
            vo.setId(dc.getId());
            vo.setDeviceId(dc.getDeviceId());
            vo.setName(dc.getName());
            vo.setData(dc.getData());
            vo.setBlocked(dc.getBlocked());
            NetworkVO networkVO = Network.convertNetwork(dc.getNetwork());
            vo.setNetworkId(networkVO.getId());
            DeviceTypeVO deviceTypeVO = DeviceType.convertDeviceType(dc.getDeviceType());
            vo.setDeviceTypeId(deviceTypeVO.getId());
        }
        return vo;
    }

    public static Device convertToEntity(DeviceVO dc) {
        Device entity = null;
        if (dc != null) {
            entity = new Device();
            entity.setId(dc.getId());
            entity.setDeviceId(dc.getDeviceId());
            entity.setName(dc.getName());
            entity.setData(dc.getData());
            entity.setBlocked(dc.getBlocked());
            Network network = new Network();
            network.setId(dc.getNetworkId());
            entity.setNetwork(network);
            DeviceType deviceType = new DeviceType();
            deviceType.setId(dc.getDeviceTypeId());
            entity.setDeviceType(deviceType);
        }
        return entity;
    }


}
