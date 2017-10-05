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
import com.devicehive.model.updates.UserUpdate;
import com.google.gson.Gson;
import io.jsonwebtoken.lang.Collections;
import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ProtocolVersion;
import org.apache.http.RequestLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.method.P;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.nio.charset.Charset;

import static com.devicehive.configuration.Constants.UTF8;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;

@Component
public class HttpRestHelper {
    private static final String AUTHORIZATION = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";
    
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
        httpPost.addHeader("Content-Type", MediaType.APPLICATION_JSON);
        if (!StringUtils.isEmpty(jsonObject)) {
            httpPost.setEntity(new StringEntity(jsonObject, Charset.forName(UTF8)));
        }
        if (!StringUtils.isEmpty(token)) {
            httpPost.addHeader(AUTHORIZATION, TOKEN_PREFIX + token);
        }
        
        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            int statusCode = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();

            if (statusCode != CREATED.getStatusCode()) {
                ErrorResponse errorResponse = gson.fromJson(EntityUtils.toString(entity), ErrorResponse.class);
                throw new HiveException(errorResponse.getMessage(), errorResponse.getError());
            }
            
            return gson.fromJson(EntityUtils.toString(entity), type);
        } catch (IOException e) {
            throw new HiveException("Auth service is not responding", BAD_REQUEST.getStatusCode());
        }
    }
    
}
