package com.devicehive.model;


import com.devicehive.json.strategies.JsonPolicyDef;
import com.google.gson.annotations.SerializedName;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;
import static com.devicehive.model.Equipment.Queries.Names;
import static com.devicehive.model.Equipment.Queries.Values;

/**
 * TODO JavaDoc
 */
@Entity
@Table(name = "equipment",
       uniqueConstraints = @UniqueConstraint(columnNames = "code"))
@NamedQueries({
                  @NamedQuery(name = Names.GET_BY_DEVICE_CLASS, query = Values.GET_BY_DEVICE_CLASS),
                  @NamedQuery(name = Names.GET_BY_DEVICE_CLASS_AND_ID, query = Values.GET_BY_DEVICE_CLASS_AND_ID),
                  @NamedQuery(name = Names.DELETE_BY_ID, query = Values.DELETE_BY_ID),
                  @NamedQuery(name = Names.DELETE_BY_DEVICE_CLASS, query = Values.DELETE_BY_DEVICE_CLASS),
                  @NamedQuery(name = Names.DELETE_BY_ID_AND_DEVICE_CLASS, query = Values.DELETE_BY_ID_AND_DEVICE_CLASS)
              })
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Equipment implements HiveEntity {

    private static final long serialVersionUID = -107312669477890926L;
    @SerializedName("id")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonPolicyDef({DEVICECLASS_PUBLISHED, EQUIPMENT_PUBLISHED, EQUIPMENTCLASS_SUBMITTED, DEVICE_PUBLISHED_DEVICE_AUTH,
                    DEVICE_PUBLISHED})
    private Long id;
    @SerializedName("name")
    @Column
    @NotNull(message = "name field cannot be null.")
    @Size(min = 1, max = 128, message = "Field cannot be empty. The length of name should not be more than 128 " +
                                        "symbols.")
    @JsonPolicyDef({EQUIPMENT_SUBMITTED, DEVICECLASS_PUBLISHED, EQUIPMENT_PUBLISHED, DEVICE_PUBLISHED_DEVICE_AUTH,
                    DEVICE_PUBLISHED, DEVICE_SUBMITTED, DEVICECLASS_SUBMITTED})
    private String name;
    @SerializedName("code")
    @Column
    @NotNull(message = "code field cannot be null.")
    @Size(min = 1, max = 128, message = "Field cannot be empty. The length of code should not be more than 128 " +
                                        "symbols.")
    @JsonPolicyDef({EQUIPMENT_SUBMITTED, DEVICECLASS_PUBLISHED, EQUIPMENT_PUBLISHED, DEVICE_PUBLISHED_DEVICE_AUTH,
                    DEVICE_PUBLISHED, DEVICE_SUBMITTED, DEVICECLASS_SUBMITTED})
    private String code;
    @SerializedName("type")
    @Column
    @NotNull(message = "type field cannot be null.")
    @Size(min = 1, max = 128, message = "Field cannot be empty. The length of type should not be more than 128 " +
                                        "symbols.")
    @JsonPolicyDef({EQUIPMENT_SUBMITTED, DEVICECLASS_PUBLISHED, EQUIPMENT_PUBLISHED, DEVICE_PUBLISHED_DEVICE_AUTH,
                    DEVICE_PUBLISHED, DEVICE_SUBMITTED, DEVICECLASS_SUBMITTED})
    private String type;
    @SerializedName("data")
    @Embedded
    @AttributeOverrides({
                            @AttributeOverride(name = "jsonString", column = @Column(name = "data"))
                        })
    @JsonPolicyDef({EQUIPMENT_SUBMITTED, DEVICECLASS_PUBLISHED, EQUIPMENT_PUBLISHED, DEVICE_PUBLISHED_DEVICE_AUTH,
                    DEVICE_PUBLISHED, DEVICE_SUBMITTED, DEVICECLASS_SUBMITTED})
    private JsonStringWrapper data;
    @ManyToOne
    @JoinColumn(name = "device_class_id", updatable = false)
    @NotNull(message = "device class field cannot be null")
    private DeviceClass deviceClass;
    @Version
    @Column(name = "entity_version")
    private long entityVersion;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getEntityVersion() {
        return entityVersion;
    }

    public void setEntityVersion(long entityVersion) {
        this.entityVersion = entityVersion;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public JsonStringWrapper getData() {
        return data;
    }

    public void setData(JsonStringWrapper data) {
        this.data = data;
    }

    public DeviceClass getDeviceClass() {
        return deviceClass;
    }

    public void setDeviceClass(DeviceClass deviceClass) {
        this.deviceClass = deviceClass;
    }

    public static interface Queries {

        public static interface Names {

            public static final String GET_BY_DEVICE_CLASS = "Equipment.getByDeviceClass";
            public static final String GET_BY_DEVICE_CLASS_AND_ID = "Equipment.getByDeviceClassAndId";
            public static final String DELETE_BY_ID = "Equipment.deleteById";
            public static final String DELETE_BY_DEVICE_CLASS = "Equipment.deleteByDeviceClass";
            public static final String DELETE_BY_ID_AND_DEVICE_CLASS = "Equipment.deleteByIdAndDeviceClass";
        }

        static interface Values {

            static final String GET_BY_DEVICE_CLASS = "select e from Equipment e where e.deviceClass = :deviceClass";
            static final String GET_BY_DEVICE_CLASS_AND_ID =
                "select e from Equipment e " +
                "join e.deviceClass dc " +
                "where e.id = :id and dc.id = :deviceClassId";
            static final String DELETE_BY_ID = "delete from Equipment e where e.id = :id";
            static final String DELETE_BY_DEVICE_CLASS = "delete from Equipment e where e.deviceClass = :deviceClass";
            static final String DELETE_BY_ID_AND_DEVICE_CLASS =
                "delete from Equipment e " +
                "where e.id = :id " +
                "and e.deviceClass in " +
                "(select dc from DeviceClass dc where dc.id = :deviceClassId)";
        }

        public static interface Parameters {

            public static final String DEVICE_CLASS = "deviceClass";
            public static final String ID = "id";
            public static final String DEVICE_CLASS_ID = "deviceClassId";

        }
    }
}
