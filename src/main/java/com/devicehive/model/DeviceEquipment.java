package com.devicehive.model;


import com.devicehive.json.strategies.JsonPolicyDef;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.ObjectUtils;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Date;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.DEVICE_EQUIPMENT_SUBMITTED;
import static com.devicehive.model.DeviceEquipment.Queries.Names;
import static com.devicehive.model.DeviceEquipment.Queries.Values;


@Entity
@Table(name = "device_equipment")
@NamedQueries({
                  @NamedQuery(name = Names.DELETE_BY_ID, query = Values.DELETE_BY_ID),
                  @NamedQuery(name = Names.GET_BY_DEVICE_AND_CODE, query = Values.GET_BY_DEVICE_AND_CODE),
                  @NamedQuery(name = Names.DELETE_BY_FK, query = Values.DELETE_BY_FK),
                  @NamedQuery(name = Names.GET_BY_DEVICE, query = Values.GET_BY_DEVICE)
              })
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class DeviceEquipment implements HiveEntity {

    private static final long serialVersionUID = 479737367629574073L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @SerializedName("sid")
    private Long id;
    @Column
    @NotNull(message = "code field cannot be null.")
    @Size(min = 1, max = 128, message = "Field cannot be empty. The length of code should not be more than 128 " +
                                        "symbols.")
    @SerializedName("id")
    @JsonPolicyDef(DEVICE_EQUIPMENT_SUBMITTED)
    private String code;
    @Column
    @NotNull
    @JsonPolicyDef(DEVICE_EQUIPMENT_SUBMITTED)
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;
    @SerializedName("parameters")
    @Embedded
    @AttributeOverrides({
                            @AttributeOverride(name = "jsonString", column = @Column(name = "parameters"))
                        })
    @JsonPolicyDef(DEVICE_EQUIPMENT_SUBMITTED)
    private JsonStringWrapper parameters;
    @ManyToOne
    @JoinColumn(name = "device_id", updatable = false)
    @NotNull(message = "device field cannot be null.")
    private Device device;
    @Version
    @Column(name = "entity_version")
    private long entityVersion;

    public long getEntityVersion() {
        return entityVersion;
    }

    public void setEntityVersion(long entityVersion) {
        this.entityVersion = entityVersion;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Date getTimestamp() {
        return ObjectUtils.cloneIfPossible(timestamp);
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = ObjectUtils.cloneIfPossible(timestamp);
    }

    public JsonStringWrapper getParameters() {
        return parameters;
    }

    public void setParameters(JsonStringWrapper parameters) {
        this.parameters = parameters;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public static class Queries {

        public interface Names {

            String DELETE_BY_ID = "DeviceEquipment.deleteById";
            String GET_BY_DEVICE_AND_CODE = "DeviceEquipment.getByDeviceAndCode";
            String DELETE_BY_FK = "DeviceEquipment.deleteByFK";
            String GET_BY_DEVICE = "DeviceEquipment.getByDevice";
        }

        interface Values {

            String DELETE_BY_ID = "delete from DeviceEquipment de where de.id = :id";
            String GET_BY_DEVICE_AND_CODE =
                "select de from DeviceEquipment de " +
                "where de.device = :device and de.code = :code";
            String DELETE_BY_FK = "delete from DeviceEquipment de where de.device = :device";
            String GET_BY_DEVICE = "select de from DeviceEquipment de where de.device = :device";
        }

        public static interface Parameters {

            static final String ID = "id";
            static final String DEVICE = "device";
            static final String CODE = "code";
        }

    }

}
