package com.devicehive.model;

import com.devicehive.json.strategies.JsonPolicyDef;
import com.google.gson.annotations.SerializedName;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

@Entity
@Table(name = "oauth_client")
@NamedQueries({
        @NamedQuery(name="OAuthClient.deleteById", query = "delete from OAuthClient oac where oac.id = :id"),
        @NamedQuery(name = "OAuthClient.getByOAuthId", query = "select oac from OAuthClient oac " +
                "where oac.oauthId = :oauthId"),
        @NamedQuery(name = "OAuthClient.getByName", query = "select oac from OAuthClient oac " +
                "where oac.name = :name"),
        @NamedQuery(name = "OAuthClient.getByOAuthIdAndSecret", query = "select oac from OAuthClient oac " +
                "where oac.oauthId = :oauthId and oac.oauthSecret = :secret")
})
@Cacheable
public class OAuthClient implements HiveEntity {

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
    @JsonPolicyDef({OAUTH_CLIENT_LISTED_ADMIN, OAUTH_CLIENT_LISTED, OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED, OAUTH_GRANT_PUBLISHED})
    private String name;

    @Column
    @SerializedName("domain")
    @Size(min = 1, max = 128, message = "Field cannot be empty. The length of domain should not be more than 128 " +
            "symbols.")
    @NotNull
    @JsonPolicyDef({OAUTH_CLIENT_LISTED_ADMIN, OAUTH_CLIENT_LISTED, OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED, OAUTH_GRANT_PUBLISHED})
    private String domain;

    @Column
    @SerializedName("subnet")
    @Size(max = 128, message = "Field cannot be empty. The length of subnet should not be more than 128 " +
            "symbols.")
    @JsonPolicyDef({OAUTH_CLIENT_LISTED_ADMIN, OAUTH_CLIENT_LISTED, OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED, OAUTH_GRANT_PUBLISHED})
    private String subnet;

    @Column(name = "redirect_uri")
    @SerializedName("redirectUri")
    @Size(min = 1, max = 128, message = "Field cannot be empty. The length of redirect URI should not be more than " +
            "128 symbols.")
    @NotNull
    @JsonPolicyDef({OAUTH_CLIENT_LISTED_ADMIN, OAUTH_CLIENT_LISTED, OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED, OAUTH_GRANT_PUBLISHED})
    private String redirectUri;

    @Column(name = "oauth_id")
    @SerializedName("oauthId")
    @Size(min = 1, max = 32, message = "Field cannot be empty. The length of oauth id should not be more " +
            "than 128 symbols.")
    @NotNull
    @JsonPolicyDef({OAUTH_CLIENT_LISTED_ADMIN, OAUTH_CLIENT_LISTED, OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED, OAUTH_GRANT_PUBLISHED})
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
}
