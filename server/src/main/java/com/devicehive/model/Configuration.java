package com.devicehive.model;

import com.google.gson.annotations.SerializedName;

import javax.persistence.*;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.sql.Date;
import java.util.HashSet;
import java.util.Set;

import static com.devicehive.model.Configuration.Queries.Names;
import static com.devicehive.model.Configuration.Queries.Values;


@Entity
@Table(name = "configuration")
@NamedQueries({
        @NamedQuery(name = Names.GET_ALL, query = Values.GET_ALL),
        @NamedQuery(name = Names.GET_BY_NAME, query = Values.GET_BY_NAME),
        @NamedQuery(name = Names.DELETE, query = Values.DELETE)
})
@Cacheable(true)
public class Configuration implements HiveEntity {


    private static final long serialVersionUID = 7957264089438389993L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    @NotNull(message = "name field cannot be null.")
    @SerializedName("name")
    @Size(min = 1, max = 32, message = "Field cannot be empty. The length of name should not be more than " +
            "32 symbols.")
    private String name;
    @Column
    @NotNull(message = "value field cannot be null.")
    @Size(min = 1, max = 128, message = "Field cannot be empty. The length of value should not be more than " +
            "128 symbols.")
    private String value;
    @Version
    @Column(name = "entity_version")
    private long entityVersion;

    public Configuration() {
    }

    public Configuration(String name, String value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Validates equipment representation. Returns set of strings which are represent constraint violations. Set will
     * be empty if no constraint violations found.
     *
     * @param configuration Equipment that should be validated
     * @param validator     Validator
     * @return Set of strings which are represent constraint violations
     */
    public static Set<String> validate(Configuration configuration, Validator validator) {
        Set<ConstraintViolation<Configuration>> constraintViolations = validator.validate(configuration);
        Set<String> result = new HashSet<>();
        if (constraintViolations.size() > 0) {
            for (ConstraintViolation<Configuration> cv : constraintViolations)
                result.add(String.format("Error! property: [%s], value: [%s], message: [%s]",
                        cv.getPropertyPath(), cv.getInvalidValue(), cv.getMessage()));
        }
        return result;

    }

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

    public String getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = Long.toString(value);
    }

    public void setValue(boolean value) {
        this.value = Boolean.toString(value);
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getValueAsInt() {
        return Integer.parseInt(value);
    }

    public long getValueAsLong() {
        return Long.parseLong(value);
    }

    public boolean getValueAsBoolean() {
        return Boolean.parseBoolean(value);
    }

    public Date getValueAsDate() {
        return Date.valueOf(value);
    }

    public void setValue(int value) {
        this.value = Integer.toString(value);
    }

    public static class Queries {
        public static interface Names {
            static final String GET_ALL = "Configuration.getAll";
            static final String GET_BY_NAME = "Configuration.getByName";
            static final String DELETE = "Configuration.delete";
        }

        static interface Values {
            public static final String GET_ALL = "select c from Configuration c";
            public static final String DELETE = "delete from Configuration c where c.name = :name";
            public static final String GET_BY_NAME = "select c from Configuration c where c.name = :name";
        }

        public static interface Parameters {
            static final String NAME = "name";
        }
    }
}
