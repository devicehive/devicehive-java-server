package com.devicehive.model;

import com.devicehive.json.strategies.JsonPolicyDef;
import com.google.gson.annotations.SerializedName;

import javax.persistence.*;
import javax.persistence.Version;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.HashSet;
import java.util.Set;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

/**
 * TODO JavaDoc
 */
@Entity
@Table(name = "network")
@NamedQueries({
        @NamedQuery(name = "Network.findByName", query = "select n from Network n where name = :name"),
        @NamedQuery(name = "Network.findWithUsers", query = "select n from Network n join fetch n.users where id = " +
                ":id"),
        @NamedQuery(name = "Network.updateById", query = "update Network n set n.description = :description where n.id = :id"),
        @NamedQuery(name = "Network.deleteById", query = "delete from Network n where n.id = :id")
})
public class Network implements HiveEntity {

    @SerializedName("id")
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @JsonPolicyDef({DEVICE_PUBLISHED,USER_PUBLISHED})
    private Long id;

    @SerializedName("key")
    @Column
    @Size(max = 64, message = "The length of key shouldn't be more than 64 symbols.")
    @JsonPolicyDef({DEVICE_PUBLISHED,USER_PUBLISHED})
    private String key;

    @SerializedName("name")
    @Column
    @NotNull(message = "name field cannot be null.")
    @Size(min = 1, max = 128, message = "Field cannot be empty. The length of name shouldn't be more than 128 symbols.")
    @JsonPolicyDef({DEVICE_PUBLISHED,DEVICE_SUBMITTED,USER_PUBLISHED})
    private String name;

    @SerializedName("description")
    @Column
    @Size(max = 128, message = "The length of description shouldn't be more than 128 symbols.")
    @JsonPolicyDef({DEVICE_PUBLISHED,DEVICE_SUBMITTED,USER_PUBLISHED})
    private String description;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_network", joinColumns = {@JoinColumn(name = "network_id", nullable = false,
            updatable = false)},
            inverseJoinColumns = {@JoinColumn(name = "user_id",nullable = false, updatable = false)})
    private Set<User> users;

    @Version
    @Column(name = "entity_version")
    private long entityVersion;

    public Network() {

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    public Set<User> getUsers() {
        return users;
    }

    public void setUsers(Set<User> users) {
        this.users = users;
    }

    public long getEntityVersion() {
        return entityVersion;
    }

    public void setEntityVersion(long entityVersion) {
        this.entityVersion = entityVersion;
    }


    /**
     * Validates network representation. Returns set of strings which are represent constraint violations. Set will
     * be empty if no constraint violations found.
     * @param network
     * Network that should be validated
     * @param validator
     * Validator
     * @return Set of strings which are represent constraint violations
     */
    public static Set<String> validate(Network network, Validator validator) {
        Set<ConstraintViolation<Network>> constraintViolations = validator.validate(network);
        Set<String> result = new HashSet<>();
        if (constraintViolations.size()>0){
            for (ConstraintViolation<Network> cv : constraintViolations)
                result.add(String.format("Error! property: [%s], value: [%s], message: [%s]",
                        cv.getPropertyPath(), cv.getInvalidValue(), cv.getMessage()));
        }
        return result;
    }

    @Override
    public boolean equals(Object o){
        if(!(o instanceof Network)){
            return false;
        }
        return this.id ==((Network) o).getId();
    }
}
