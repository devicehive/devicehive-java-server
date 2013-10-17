package com.devicehive.model.updates;

import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.HiveEntity;
import com.devicehive.model.NullableWrapper;
import com.devicehive.model.OAuthClient;
import com.google.gson.annotations.SerializedName;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.OAUTH_CLIENT_PUBLISHED;

public class OAuthClientUpdate implements HiveEntity {
    private static final long serialVersionUID = -5522057352642320219L;
    @SerializedName("id")
    private Long id;

    @SerializedName("name")
    @JsonPolicyDef({OAUTH_CLIENT_PUBLISHED})
    private NullableWrapper<String> name;

    @SerializedName("domain")
    @JsonPolicyDef({OAUTH_CLIENT_PUBLISHED})
    private NullableWrapper<String> domain;

    @SerializedName("subnet")
    @JsonPolicyDef({OAUTH_CLIENT_PUBLISHED})
    private NullableWrapper<String> subnet;

    @SerializedName("redirectUri")
    @JsonPolicyDef({OAUTH_CLIENT_PUBLISHED})
    private NullableWrapper<String> redirectUri;

    @SerializedName("oauthId")
    @JsonPolicyDef({OAUTH_CLIENT_PUBLISHED})
    private NullableWrapper<String> oauthId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public NullableWrapper<String> getName() {
        return name;
    }

    public void setName(NullableWrapper<String> name) {
        this.name = name;
    }

    public NullableWrapper<String> getDomain() {
        return domain;
    }

    public void setDomain(NullableWrapper<String> domain) {
        this.domain = domain;
    }

    public NullableWrapper<String> getSubnet() {
        return subnet;
    }

    public void setSubnet(NullableWrapper<String> subnet) {
        this.subnet = subnet;
    }

    public NullableWrapper<String> getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(NullableWrapper<String> redirectUri) {
        this.redirectUri = redirectUri;
    }

    public NullableWrapper<String> getOauthId() {
        return oauthId;
    }

    public void setOauthId(NullableWrapper<String> oauthId) {
        this.oauthId = oauthId;
    }

    public OAuthClient convertTo(){
        OAuthClient client  =  new OAuthClient();
        client.setId(id);
        if (name != null){
            client.setName(name.getValue());
        }
        if (domain != null){
            client.setDomain(domain.getValue());
        }
        if (subnet != null){
            client.setSubnet(subnet.getValue());
        }
        if (redirectUri != null){
            client.setRedirectUri(redirectUri.getValue());
        }
        if (oauthId != null){
            client.setOauthId(oauthId.getValue());
        }
        return client;
    }
}
