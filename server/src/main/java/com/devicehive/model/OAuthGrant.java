package com.devicehive.model;

import com.devicehive.exceptions.HiveException;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.ObjectUtils;

import javax.persistence.*;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

@Entity
@Table(name = "oauth_grant")
@NamedQueries({
        @NamedQuery(name = "OAuthGrant.getByIdAndUser", query = "select oag " +
                "from OAuthGrant oag " +
                "join fetch oag.client " +
                "join fetch oag.accessKey ak " +
                "join fetch ak.permissions " +
                "join fetch oag.user " +
                "where oag.id = :grantId and oag.user = :user"),
        @NamedQuery(name = "OAuthGrant.getById", query = "select oag " +
                "from OAuthGrant oag " +
                "join fetch oag.client " +
                "join fetch oag.accessKey ak " +
                "join fetch ak.permissions " +
                "join fetch oag.user " +
                "where oag.id = :grantId"),
        @NamedQuery(name = "OAuthGrant.deleteByUserAndId",
                query = "delete from OAuthGrant oag where oag.id = :grantId and oag.user = :user"),
        @NamedQuery(name = "OAuthGrant.deleteById", query = "delete from OAuthGrant oag where oag.id = :grantId"),
        @NamedQuery(name = "OAuthGrant.getByCodeAndOAuthID", query = "select oag " +
                "from OAuthGrant oag " +
                "join fetch oag.client c " +
                "join fetch oag.accessKey ak " +
                "join fetch ak.permissions " +
                "where oag.authCode = :authCode and c.oauthId = :oauthId")
})
@Cacheable
public class OAuthGrant implements HiveEntity {

    private static final long serialVersionUID = 6725932065321755993L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @SerializedName("id")
    @JsonPolicyDef({OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED, OAUTH_GRANT_SUBMITTED_TOKEN, OAUTH_GRANT_SUBMITTED_CODE})
    private Long id;

    @SerializedName("timestamp")
    @Column
    @JsonPolicyDef({OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED, OAUTH_GRANT_SUBMITTED_TOKEN, OAUTH_GRANT_SUBMITTED_CODE})
    private Timestamp timestamp;

    @SerializedName("authCode")
    @Size(min = 35, max = 36, message = "Field cannot be empty. The length of authCode should be 36 symbols.")
    @Column(name = "auth_code")
    @JsonPolicyDef({OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED, OAUTH_GRANT_SUBMITTED_CODE})
    private String authCode;

    @SerializedName("client")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    @NotNull(message = "client field cannot be null")
    @JsonPolicyDef({OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED, OAUTH_GRANT_PUBLISHED})
    private OAuthClient client;

    @SerializedName("accessKey")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "key_id")
    @NotNull(message = "access key field cannot be null")
    @JsonPolicyDef({OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED, OAUTH_GRANT_SUBMITTED_TOKEN})
    private AccessKey accessKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", updatable = false)
    @NotNull(message = "user field cannot be null")
    private User user;

    @Column
    @SerializedName("type")
    @JsonPolicyDef({OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED, OAUTH_GRANT_PUBLISHED})
    private Type type;

    @Column(name = "access_type")
    @SerializedName("accessType")
    @JsonPolicyDef({OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED, OAUTH_GRANT_PUBLISHED})
    private AccessType accessType;

    @Column(name = "redirect_uri")
    @SerializedName("redirectUri")
    @Size(min = 1, max = 128, message = "Field cannot be empty. The length of redirect uri should not be more that " +
            "128 symbols.")
    @NotNull(message = "Redirect uri field cannot be null")
    @JsonPolicyDef({OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED, OAUTH_GRANT_PUBLISHED})
    private String redirectUri;

    @Column
    @SerializedName("scope")
    @Size(min = 1, max = 128, message = "Field cannot be empty. The length of redirect uri should not be more that " +
            "128 symbols.")
    @NotNull(message = "Redirect uri field cannot be null")
    @JsonPolicyDef({OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED, OAUTH_GRANT_PUBLISHED})
    private String scope;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "jsonString", column = @Column(name = "network_ids"))
    })
    @JsonPolicyDef({OAUTH_GRANT_LISTED_ADMIN, OAUTH_GRANT_LISTED, OAUTH_GRANT_PUBLISHED})
    private JsonStringWrapper networkIds;

    @Version
    @Column(name = "entity_version")
    private long entityVersion;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Timestamp getTimestamp() {
        return ObjectUtils.cloneIfPossible(timestamp);
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = ObjectUtils.cloneIfPossible(timestamp);
    }

    public String getAuthCode() {
        return authCode;
    }

    public void setAuthCode(String authCode) {
        this.authCode = authCode;
    }

    public OAuthClient getClient() {
        return client;
    }

    public void setClient(OAuthClient client) {
        this.client = client;
    }

    public AccessKey getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(AccessKey accessKey) {
        this.accessKey = accessKey;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public AccessType getAccessType() {
        return accessType;
    }

    public void setAccessType(AccessType accessType) {
        this.accessType = accessType;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public JsonStringWrapper getNetworkIds() {
        return networkIds;
    }

    public void setNetworkIds(JsonStringWrapper networkIds) {
        this.networkIds = networkIds;
    }

    public long getEntityVersion() {
        return entityVersion;
    }

    public void setEntityVersion(long entityVersion) {
        this.entityVersion = entityVersion;
    }

    public Set<Long> getNetworkIdsAsSet() {
        if (networkIds == null) {
            return null;
        }
        JsonParser parser = new JsonParser();
        JsonElement elem = parser.parse(networkIds.getJsonString());
        if (elem instanceof JsonNull) {
            return null;
        }
        if (elem instanceof JsonArray) {
            JsonArray json = (JsonArray) elem;
            Set<Long> result = new HashSet<>(json.size());
            for (JsonElement current : json) {
                result.add(current.getAsLong());
            }
            return result;
        }
        throw new HiveException("JSON array expected!", HttpServletResponse.SC_BAD_REQUEST);
    }
}
