package com.devicehive.service.helpers;

/*
 * #%L
 * DeviceHive Frontend Logic
 * %%
 * Copyright (C) 2016 - 2017 DataArt
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


import com.devicehive.exceptions.HiveException;
import com.devicehive.model.ErrorResponse;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.charset.Charset;

import static com.devicehive.configuration.Constants.UTF8;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;

@Component
public class HttpRestHelper {
    private static final String AUTHORIZATION = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    
    private static final PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
    private CloseableHttpClient httpClient;
    
    private final Gson gson;

    @Autowired
    public HttpRestHelper(Gson gson) {
        this.gson = gson;
    }

    @PostConstruct
    private void init() {
        cm.setMaxTotal(100);

        httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .build();
    }

    public <T> T post(String url, String jsonObject, Class<T> type, String token) {
        HttpPost httpPost = new HttpPost(url);
        addHeaders(httpPost, token);

        if (!StringUtils.isEmpty(jsonObject)) {
            httpPost.setEntity(new StringEntity(jsonObject, Charset.forName(UTF8)));
        }
        
        return httpRequest(httpPost, type, CREATED);
    }

    public <T> T get(String url, Class<T> type, String token) {
        HttpGet httpGet = new HttpGet(url);
        addHeaders(httpGet, token);

        return httpRequest(httpGet, type, OK);
    }

    private void addHeaders(HttpRequestBase httpRequest, String token) {
        httpRequest.addHeader("Content-Type", MediaType.APPLICATION_JSON);
        if (!StringUtils.isEmpty(token)) {
            String authorization = token.startsWith(TOKEN_PREFIX) ? token : TOKEN_PREFIX + token;
            httpRequest.addHeader(AUTHORIZATION, authorization);
        }
    }

    private <T> T httpRequest(HttpRequestBase httpRequestBase, Class<T> type, Response.Status status) {
        try (CloseableHttpResponse response = httpClient.execute(httpRequestBase)) {
            int statusCode = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();

            if (statusCode != status.getStatusCode()) {
                ErrorResponse errorResponse = null;
                try {
                    errorResponse = gson.fromJson(EntityUtils.toString(entity), ErrorResponse.class);
                } catch (Exception e) {
                    throw new HiveException("Invalid response status");
                }
                throw new HiveException(errorResponse.getMessage(), errorResponse.getError());
            }
            
            return gson.fromJson(EntityUtils.toString(entity), type);
        } catch (JsonSyntaxException e) {
            throw new ServiceUnavailableException("Service response type is wrong");
        } catch (IOException e) {
            throw new ServiceUnavailableException("Service is not available");
        }
    }

}
