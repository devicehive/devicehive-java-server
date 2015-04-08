package com.devicehive.model;

import com.google.gson.annotations.SerializedName;

import com.devicehive.json.strategies.JsonPolicyDef;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Version;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.OAUTH_CLIENT_LISTED;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.OAUTH_CLIENT_LISTED_ADMIN;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.OAUTH_CLIENT_PUBLISHED;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.OAUTH_GRANT_LISTED;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.OAUTH_GRANT_LISTED_ADMIN;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.OAUTH_GRANT_PUBLISHED;
import static com.devicehive.model.OAuthClient.Queries.Names;
import static com.devicehive.model.OAuthClient.Queries.Values;

@Entity
@Table(name = "oauth_client")
@NamedQueries({
                  @NamedQuery(name = Names.DELETE_BY_ID, query = Values.DELETE_BY_ID),
                  @NamedQuery(name = Names.GET_BY_OAUTH_ID, query = Values.GET_BY_OAUTH_ID),
                  @NamedQuery(name = Names.GET_BY_NAME, query = Values.GET_BY_NAME),
                  @NamedQuery(name = Names.GET_BY_OAUTH_ID_AND_SECRET, query = Values.GET_BY_OAUTH_ID_AND_SECRET)
              })
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class OAuthClient implements HiveEntity {

    public static final String NAME_COLUMN = "name";
    public static final String DOMAIN_COLUMN = "domain";
    public static final String OAUTH_ID_COLUMN = "oauthId";
    private static final long serialVersionUID = -1095382534684298244L;
    @Id
    @SerializedName("id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonPolicyDef({OAUTH_CLIENT_LISTED_ADMIN, OAUTH_CLIENT_LISTED, OAUTH_CLIENT_PUBLISHED, OAUTH_GRANT_LISTED_ADMIN,
                    OAUTH_GRANT_LISTED})
    private Long id;
    @Column
    @SerializedName("name")
    @Size(min = 1, max = 128, message = "Field cannot be empty. The length of name should not be more than 128 " +
                                        "symbols.")
    @NotNull
    @JsonPolicyDef({OAUTH_CLIENT_LISTED_ADMIN, OAUTH_CLIENT_LISTED, OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED,
                    OAUTH_GRANT_PUBLISHED})
    private String name;
    @Column
    @SerializedName("domain")
    @Size(min = 1, max = 128, message = "Field cannot be empty. The length of domain should not be more than 128 " +
                                        "symbols.")
    @NotNull
    @JsonPolicyDef({OAUTH_CLIENT_LISTED_ADMIN, OAUTH_CLIENT_LISTED, OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED,
                    OAUTH_GRANT_PUBLISHED})
    private String domain;
    @Column
    @SerializedName("subnet")
    @Size(max = 128, message = "Field cannot be empty. The length of subnet should not be more than 128 " +
                               "symbols.")
    @JsonPolicyDef({OAUTH_CLIENT_LISTED_ADMIN, OAUTH_CLIENT_LISTED, OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED,
                    OAUTH_GRANT_PUBLISHED})
    private String subnet;
    @Column(name = "redirect_uri")
    @SerializedName("redirectUri")
    @Size(min = 1, max = 128, message = "Field cannot be empty. The length of redirect URI should not be more than " +
                                        "128 symbols.")
    @NotNull
    @JsonPolicyDef({OAUTH_CLIENT_LISTED_ADMIN, OAUTH_CLIENT_LISTED, OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED,
                    OAUTH_GRANT_PUBLISHED})
    private String redirectUri;
    @Column(name = "oauth_id")
    @SerializedName("oauthId")
    @Size(min = 1, max = 32, message = "Field cannot be empty. The length of oauth id should not be more " +
                                       "than 128 symbols.")
    @NotNull
    @JsonPolicyDef({OAUTH_CLIENT_LISTED_ADMIN, OAUTH_CLIENT_LISTED, OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED,
                    OAUTH_GRANT_PUBLISHED})
    private String oauthId;
    @Column(name = "oauth_secret")
    @SerializedName("oauthSecret")
    @Size(min = 24, max = 32, message = "Field cannot be empty. The length of oauth secret should not be " +
                                        "more than 128 symbols.")
    @NotNull
    @JsonPolicyDef({OAUTH_CLIENT_LISTED_ADMIN, OAUTH_CLIENT_PUBLISHED, OAUTH_GRANT_LISTED_ADMIN})
    private String oauthSecret;
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

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getSubnet() {
        return subnet;
    }

    public void setSubnet(String subnet) {
        this.subnet = subnet;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getOauthId() {
        return oauthId;
    }

    public void setOauthId(String oauthId) {
        this.oauthId = oauthId;
    }

    public String getOauthSecret() {
        return oauthSecret;
    }

    public void setOauthSecret(String oauthSecret) {
        this.oauthSecret = oauthSecret;
    }

    public static class Queries {

        public static interface Names {

            static final String DELETE_BY_ID = "OAuthClient.deleteById";
            static final String GET_BY_OAUTH_ID = "OAuthClient.getByOAuthId";
            static final String GET_BY_NAME = "OAuthClient.getByName";
            static final String GET_BY_OAUTH_ID_AND_SECRET = "OAuthClient.getByOAuthIdAndSecret";
        }

        static interface Values {

            static final String DELETE_BY_ID = "delete from OAuthClient oac where oac.id = :id";
            static final String GET_BY_OAUTH_ID = "select oac from OAuthClient oac where oac.oauthId = :oauthId";
            static final String GET_BY_NAME = "select oac from OAuthClient oac where oac.name = :name";
            static final String GET_BY_OAUTH_ID_AND_SECRET =
                "select oac from OAuthClient oac " +
                "where oac.oauthId = :oauthId and oac.oauthSecret = :secret";
        }

        public static interface Parameters {

            static final String ID = "id";
            static final String OAUTH_ID = "oauthId";
            static final String NAME = "name";
            static final String SECRET = "secret";
        }
    }
}
