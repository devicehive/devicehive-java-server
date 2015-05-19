package com.devicehive.model.oauth;

import com.devicehive.configuration.Messages;
import com.devicehive.exceptions.HiveException;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.*;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Singleton;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import static com.devicehive.configuration.Constants.UTF8;

/**
 * Created by tmatvienko on 1/9/15.
 */
@Singleton
public class IdentityProviderUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdentityProviderUtils.class);

    public JsonElement executeGet(final HttpTransport transport, final Credential.AccessMethod accessMethod, final String accessToken,
                                  final String endpoint, final String providerName) {
        LOGGER.debug("executeGet: endpoint {}, providerName {}", endpoint, providerName);
        try {
            final GenericUrl url = new GenericUrl(endpoint);
            final HttpRequestFactory requestFactory = StringUtils.isNotBlank(accessToken) ?
                    transport.createRequestFactory(new Credential(accessMethod).setAccessToken(accessToken)) :
                    transport.createRequestFactory();
            final String response = requestFactory.buildGetRequest(url).execute().parseAsString();
            JsonElement jsonElement = new JsonParser().parse(response);
            LOGGER.debug("executeGet response: {}", jsonElement);
            try {
                final JsonElement error = jsonElement.getAsJsonObject().get("error");
                if (error != null) {
                    LOGGER.error("Exception has been caught during Identity Provider GET request execution", error);
                    throw new HiveException(String.format(Messages.OAUTH_ACCESS_TOKEN_VERIFICATION_FAILED, providerName, error),
                            Response.Status.FORBIDDEN.getStatusCode());
                }
            } catch (IllegalStateException ex) {
                return jsonElement;
            }
            return jsonElement;
        } catch (IOException e) {
            LOGGER.error("Exception has been caught during Identity Provider GET request execution", e);
            throw new HiveException(Messages.IDENTITY_PROVIDER_API_REQUEST_ERROR, Response.Status.UNAUTHORIZED.getStatusCode());
        }
    }

    public String executeGet(final HttpTransport transport, final List<NameValuePair> params,
                             final String endpoint, final String providerName) {
        LOGGER.debug("executeGet: endpoint {}, providerName {}", endpoint, providerName);
        try {
            final HttpRequestFactory requestFactory = transport.createRequestFactory();
            final String paramString = URLEncodedUtils.format(params, Charset.forName(UTF8));
            final GenericUrl url = new GenericUrl(String.format("%s?%s", endpoint, paramString));
            final HttpRequest request = requestFactory.buildGetRequest(url);
            return request.execute().parseAsString();
        } catch (IOException e) {
            LOGGER.error("Exception has been caught during Identity Provider GET request execution", e);
            throw new HiveException(Messages.IDENTITY_PROVIDER_API_REQUEST_ERROR, Response.Status.SERVICE_UNAVAILABLE.getStatusCode());
        }
    }

    public String executePost(final HttpTransport transport, final Map<String, String> params,
                              final String endpoint, final String providerName) {
        LOGGER.debug("executePost: endpoint {}, providerName {}", endpoint, providerName);
        try {
            final HttpRequestFactory requestFactory = transport.createRequestFactory();
            final GenericUrl url = new GenericUrl(endpoint);
            final HttpContent httpContent = new UrlEncodedContent(params);
            final HttpRequest request = requestFactory.buildPostRequest(url, httpContent);
            final HttpHeaders headers = new HttpHeaders().setContentType("application/x-www-form-urlencoded");
            return request.setHeaders(headers).execute().parseAsString();
        } catch (IOException e) {
            LOGGER.error("Exception has been caught during Identity Provider POST request execution", e);
            throw new HiveException(Messages.IDENTITY_PROVIDER_API_REQUEST_ERROR, Response.Status.SERVICE_UNAVAILABLE.getStatusCode());
        }
    }
}
