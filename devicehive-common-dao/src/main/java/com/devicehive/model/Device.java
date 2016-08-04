package com.devicehive.model;

import com.basho.riak.client.api.annotations.RiakIndex;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.vo.*;
import com.google.gson.annotations.SerializedName;
import io.swagger.annotations.ApiModelProperty;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

/**
 * TODO JavaDoc
 */


@Entity
@Table(name = "device")
@NamedQueries({
                  @NamedQuery(name = "Device.findByUUID", query = "select d from Device d " +
                                                                  "left join fetch d.network " +
                                                                  "left join fetch d.deviceClass dc " +
                                                                  "left join fetch dc.equipment " +
                                                                  "where d.guid = :guid"),
                  @NamedQuery(name = "Device.deleteByUUID", query = "delete from Device d where d.guid = :guid")
              })
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Device implements HiveEntity {

    public static final String NETWORK_COLUMN = "network";
    public static final String GUID_COLUMN = "guid";

    private static final long serialVersionUID = 2959997451631843298L;

    @Id
    @SerializedName("sid")//overwork for  "declares multiple JSON fields" exception
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @SerializedName("id")
    @Column
    @NotNull(message = "guid field cannot be null.")
    @Size(min = 1, max = 48, message = "Field cannot be empty. The length of guid should not be more than 48 symbols.")
    @JsonPolicyDef({DEVICE_PUBLISHED, NETWORK_PUBLISHED})
    private String guid;

    @SerializedName("name")
    @Column
    @NotNull(message = "name field cannot be null.")
    @Size(min = 1, max = 128, message = "Field cannot be empty. The length of name should not be more than 128 " +
                                        "symbols.")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED})
    private String name;

    @SerializedName("status")
    @Column
    @Size(min = 1, max = 128,
          message = "Field cannot be empty. The length of status should not be more than 128 symbols.")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED})
    private String status;

    @SerializedName("data")
    @Embedded
    @AttributeOverrides({
                            @AttributeOverride(name = "jsonString", column = @Column(name = "data"))
                        })
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED})
    private JsonStringWrapper data;

    @SerializedName("network")
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "network_id")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED})
    private Network network;

    @SerializedName("deviceClass")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_class_id")
    @NotNull(message = "deviceClass field cannot be null.")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED})
    private DeviceClass deviceClass;

    @Column(name = "blocked")
    @SerializedName("isBlocked")
    @ApiModelProperty(name="isBlocked")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, NETWORK_PUBLISHED})
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

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Network getNetwork() {
        return network;
    }

    public void setNetwork(Network network) {
        this.network = network;
    }

    public DeviceClass getDeviceClass() {
        return deviceClass;
    }

    public void setDeviceClass(DeviceClass deviceClass) {
        this.deviceClass = deviceClass;
    }

    public Boolean getBlocked() {
        return blocked;
    }

    public void setBlocked(Boolean blocked) {
        this.blocked = blocked;
    }

    //Riak indexes
    @RiakIndex(name = "guid")
    public String getGuidSi() {
        return guid;
    }

    public static class Queries {

        public static interface Parameters {

            static final String GUID = "guid";
            static final String ID = "id";
        }
    }

    public static DeviceVO convertToVo(Device dc) {
        DeviceVO vo = null;
        if (dc != null) {
            vo = new DeviceVO();
            vo.setId(dc.getId());
            vo.setBlocked(dc.getBlocked());
            vo.setData(dc.getData());
            //TODO ???vo.setDeviceClass();
            DeviceClassVO classVO = DeviceClass.convertToVo(dc.getDeviceClass());
            vo.setDeviceClass(classVO);
            vo.setGuid(dc.getGuid());
            vo.setId(dc.getId());
            vo.setName(dc.getName());
            //TODO Network convert = Network.convertNetwork(dc.getNetwork());
            NetworkVO networkVO = Network.convertNetwork(dc.getNetwork());
            vo.setNetwork(networkVO);
            vo.setStatus(dc.getStatus());
        }
        return vo;
    }

    public static Device convertToEntity(DeviceVO dc) {
        Device entity = null;
        if (dc != null) {
            entity = new Device();
            entity.setId(dc.getId());
            entity.setBlocked(dc.getBlocked());
            entity.setData(dc.getData());
            //TODO ??? next lines conversion
            DeviceClass deviceClass = DeviceClass.convertToEntity(dc.getDeviceClass());
            entity.setDeviceClass(deviceClass);
            entity.setGuid(dc.getGuid());
            entity.setId(dc.getId());
            entity.setName(dc.getName());
            //TODO ???vo.setNetwork();
            Network network = Network.convert(dc.getNetwork());
            entity.setNetwork(network);
            entity.setStatus(dc.getStatus());
        }
        return entity;
    }


}
