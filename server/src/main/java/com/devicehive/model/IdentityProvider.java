package com.devicehive.model;

import com.devicehive.json.strategies.JsonPolicyDef;
import com.google.gson.annotations.SerializedName;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.IDENTITY_PROVIDER_LISTED;
import static com.devicehive.model.IdentityProvider.Queries.Names;
import static com.devicehive.model.IdentityProvider.Queries.Values;

/**
 * Created by tmatvienko on 11/17/14.
 */
@Entity
@Table(name = "identity_provider")
@NamedQueries({
        @NamedQuery(name = Names.GET_BY_NAME, query = Values.GET_BY_NAME),
        @NamedQuery(name = Names.DELETE_BY_ID, query = Values.DELETE_BY_ID)
})
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class IdentityProvider implements HiveEntity {

    private static final long serialVersionUID = 1959997436981843212L;
    @Id
    @SerializedName("id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonPolicyDef({IDENTITY_PROVIDER_LISTED})
    private Long id;

    @Column
    @NotNull(message = "name field cannot be null.")
    @Size(min = 1, max = 64, message = "Field cannot be empty. The length of login should not be more than 64 " +
            "symbols.")
    @SerializedName("name")
    @JsonPolicyDef({IDENTITY_PROVIDER_LISTED})
    private String name;

    @Column(name = "api_endpoint")
    @NotNull(message = "identity provider's api endpoint can't be null.")
    @SerializedName("apiEndpoint")
    @JsonPolicyDef({IDENTITY_PROVIDER_LISTED})
    private String apiEndpoint;

    @Column(name = "verification_endpoint")
    @NotNull(message = "identity provider's verification endpoint can't be null.")
    @SerializedName("verificationEndpoint")
    @JsonPolicyDef({IDENTITY_PROVIDER_LISTED})
    private String verificationEndpoint;

    @Column(name = "token_endpoint")
    @NotNull(message = "identity provider's access token endpoint can't be null.")
    @SerializedName("tokenEndpoint")
    @JsonPolicyDef({IDENTITY_PROVIDER_LISTED})
    private String tokenEndpoint;

    @Version
    @Column(name = "entity_version")
    private long entityVersion;

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

    public String getApiEndpoint() {
        return apiEndpoint;
    }

    public void setApiEndpoint(String apiEndpoint) {
        this.apiEndpoint = apiEndpoint;
    }

    public String getVerificationEndpoint() {
        return verificationEndpoint;
    }

    public void setVerificationEndpoint(String verificationEndpoint) {
        this.verificationEndpoint = verificationEndpoint;
    }

    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    public void setTokenEndpoint(String tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }

    public long getEntityVersion() {
        return entityVersion;
    }

    public void setEntityVersion(long entityVersion) {
        this.entityVersion = entityVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        IdentityProvider user = (IdentityProvider) o;

        return id != null && id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }

    public static class Queries {

        public static interface Names {

            static final String GET_BY_NAME = "IdentityProvider.getByName";
            static final String DELETE_BY_ID = "IdentityProvider.deleteById";
        }

        static interface Values {

            static final String GET_BY_NAME = "select ip from IdentityProvider ip where ip.name = :name";
            static final String DELETE_BY_ID = "delete from IdentityProvider ip where ip.id = :id";
        }

        public static interface Parameters {

            static final String ID = "id";
            static final String NAME = "name";
        }
    }
}
