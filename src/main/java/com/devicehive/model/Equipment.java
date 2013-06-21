package com.devicehive.model;

import com.devicehive.model.converters.JsonConverter;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import javax.persistence.*;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.HashSet;
import java.util.Set;

/**
 * TODO JavaDoc
 */
@Entity
@Table(name = "equipment")
public class Equipment {
    @SerializedName("id")

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @SerializedName("name")
    @Column
    @NotNull(message = "name field cannot be null.")
    @Size(min = 1, max = 128, message = "Field cannot be empty. The length of name shouldn't be more than 128 symbols.")
    private String name;

    @SerializedName("code")
    @Column
    @NotNull(message = "code field cannot be null.")
    @Size(min = 1, max = 128, message = "Field cannot be empty. The length of code shouldn't be more than 128 symbols.")
    private String code;

    @SerializedName("type")
    @Column
    @NotNull(message = "type field cannot be null.")
    @Size(min = 1, max = 128, message = "Field cannot be empty. The length of type shouldn't be more than 128 symbols.")
    private String type;

    @SerializedName("data")
    @Column
    @Convert(converter = JsonConverter.class)   //TODO??
    private JsonElement data;

    @ManyToOne
    @JoinColumn(name = "device_class_id")
    @NotNull(message = "device class field cannot be null")
    private DeviceClass deviceClass;

    public Equipment() {
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

    public JsonElement getData() {
        return data;
    }

    public void setData(JsonElement data) {
        this.data = data;
    }

    public DeviceClass getDeviceClass() {
        return deviceClass;
    }

    public void setDeviceClass(DeviceClass deviceClass) {
        this.deviceClass = deviceClass;
    }

    /**
     * Validates equipment representation. Returns set of strings which are represent constraint violations. Set will
     * be empty if no constraint violations found.
     * @param equipment
     * Equipment that should be validated
     * @param validator
     * Validator
     * @return Set of strings which are represent constraint violations
     */
    public static Set<String> validate(Equipment equipment, Validator validator) {
        Set<ConstraintViolation<Equipment>> constraintViolations = validator.validate(equipment);
        Set<String> result = new HashSet<>();
        if (constraintViolations.size()>0){
            for (ConstraintViolation<Equipment> cv : constraintViolations)
                result.add(String.format("Error! property: [%s], value: [%s], message: [%s]",
                        cv.getPropertyPath(), cv.getInvalidValue(), cv.getMessage()));
        }
        return result;

    }
}
