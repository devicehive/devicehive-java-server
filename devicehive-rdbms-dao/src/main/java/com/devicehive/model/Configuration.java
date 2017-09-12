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

import com.devicehive.vo.ConfigurationVO;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.HashSet;
import java.util.Optional;
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
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Configuration implements HiveEntity {


    private static final long serialVersionUID = 7957264089438389993L;


    @Id
    @Column(unique = true)
    @NotNull(message = "name field cannot be null.")
    @SerializedName("name")
    @Size(min = 1, max = 32, message = "Field cannot be empty. The length of name should not be more than " +
                                       "32 symbols.")
    @JsonProperty
    private String name;
    @Column
    @NotNull(message = "value field cannot be null.")
    @Size(min = 1, max = 128, message = "Field cannot be empty. The length of value should not be more than " +
                                        "128 symbols.")
    @JsonProperty
    private String value;
    @Version
    @Column(name = "entity_version")
    @JsonProperty
    private long entityVersion;

    public Configuration() {
    }

    public Configuration(String name, String value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Validates equipment representation. Returns set of strings which are represent constraint violations. Set will be
     * empty if no constraint violations found.
     *
     * @param configuration Equipment that should be validated
     * @param validator     Validator
     * @return Set of strings which are represent constraint violations
     */
    public static Set<String> validate(Configuration configuration, Validator validator) {
        Set<ConstraintViolation<Configuration>> constraintViolations = validator.validate(configuration);
        Set<String> result = new HashSet<>();
        if (constraintViolations.size() > 0) {
            for (ConstraintViolation<Configuration> cv : constraintViolations) {
                result.add(String.format("Error! property: [%s], value: [%s], message: [%s]",
                                         cv.getPropertyPath(), cv.getInvalidValue(), cv.getMessage()));
            }
        }
        return result;

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

    @JsonIgnore
    public void setValue(long value) {
        this.value = Long.toString(value);
    }

    @JsonIgnore
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

    @JsonIgnore
    public void setValue(int value) {
        this.value = Integer.toString(value);
    }

    public static class Queries {

        public interface Names {

            String GET_ALL = "Configuration.getAll";
            String GET_BY_NAME = "Configuration.getByName";
            String DELETE = "Configuration.delete";
        }

        interface Values {

            String GET_ALL = "select c from Configuration c";
            String DELETE = "delete from Configuration c where c.name = :name";
            String GET_BY_NAME = "select c from Configuration c where c.name = :name";
        }

        public interface Parameters {

            String NAME = "name";
        }
    }

    public static Configuration convert(ConfigurationVO vo) {
        if (vo != null) {
            Configuration result = new Configuration();
            result.setName(vo.getName());
            result.setValue(vo.getValue());
            result.setEntityVersion(vo.getEntityVersion());
            return result;
        } else {
            return null;
        }
    }

    public static ConfigurationVO convert(Configuration configuration) {
        if (configuration != null) {
            ConfigurationVO vo = new ConfigurationVO();
            vo.setName(configuration.getName());
            vo.setValue(configuration.getValue());
            vo.setEntityVersion(configuration.getEntityVersion());
            return vo;
        } else {
            return null;
        }
    }

    public static Optional<ConfigurationVO> convert(Optional<Configuration> configuration) {
        if (configuration != null) {
            if (configuration.isPresent()) {
                return Optional.ofNullable(convert(configuration.get()));
            } else {
                return Optional.empty();
            }
        } else {
            return null;
        }
    }
}
