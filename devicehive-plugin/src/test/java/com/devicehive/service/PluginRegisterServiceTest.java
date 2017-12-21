package com.devicehive.service;

/*
 * #%L
 * DeviceHive Common Module
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

import com.devicehive.auth.HivePrincipal;
import com.devicehive.model.eventbus.Filter;
import com.devicehive.model.rpc.PluginSubscribeRequest;
import com.devicehive.model.updates.PluginUpdate;
import com.devicehive.proxy.config.WebSocketKafkaProxyConfig;
import com.devicehive.service.helpers.HttpRestHelper;
import com.devicehive.service.helpers.LongIdGenerator;
import com.devicehive.service.helpers.ResponseConsumer;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;
import com.devicehive.shim.kafka.topic.KafkaTopicService;
import com.devicehive.util.HiveValidator;
import com.devicehive.vo.JwtTokenVO;
import com.devicehive.vo.UserVO;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class PluginRegisterServiceTest {
    private static final Long USER_ID = 1L;
    private static final Long SUBSCRIPTION_ID = 1L;
    private static final String ACCESS_TOKEN = "accessToken";
    private static final String REFRESH_TOKEN = "refreshToken";
    private static final String PROXY_ENDPOINT = "proxyEndpoint";
    private static final String AUTHORIZATION = "authorization";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private HiveValidator hiveValidator;
    @Mock
    private PluginService pluginService;
    @Mock
    private RpcClient rpcClient;
    @Mock
    private KafkaTopicService kafkaTopicService;
    @Mock
    private LongIdGenerator idGenerator;
    @Mock
    private BaseDeviceService deviceService;
    @Mock
    private HttpRestHelper httpRestHelper;
    @Mock
    private WebSocketKafkaProxyConfig webSocketKafkaProxyConfig;
    @Mock
    private Gson gson;
    
    @InjectMocks
    PluginRegisterService pluginRegisterService;
    
    
    @Test
    public void shoultRegisterPlugin() throws Exception {
        // TODO - fix with new method api
//        //given
//        PluginSubscribeRequest pollRequest = new PluginSubscribeRequest();
//        pollRequest.setFilters(Collections.singleton(new Filter()));
//
//        PluginUpdate pluginUpdate = new PluginUpdate();
//
//        given(idGenerator.generate()).willReturn(SUBSCRIPTION_ID);
//        given(webSocketKafkaProxyConfig.getProxyConnect()).willReturn(PROXY_ENDPOINT);
//        given(httpRestHelper.post(any(), any(), any(), any())).willReturn(createJwtTokenVO(ACCESS_TOKEN, REFRESH_TOKEN));
//
//        doAnswer(invocation -> {
//            Object[] args = invocation.getArguments();
//            Request request = (Request)args[0];
//            ResponseConsumer responseConsumer = (ResponseConsumer)args[1];
//            responseConsumer.accept(Response.newBuilder()
//                    .withBody(request.getBody())
//                    .buildSuccess());
//
//            return null;
//        }).when(rpcClient).call(any(), any());
//
//        //when
//        JsonObject actual = (JsonObject) pluginRegisterService.register(pollRequest, pluginUpdate, AUTHORIZATION).join().getEntity();
//
//        //then
//        assertEquals(actual.get(ACCESS_TOKEN).getAsString(), ACCESS_TOKEN);
//        assertEquals(actual.get(REFRESH_TOKEN).getAsString(), REFRESH_TOKEN);
//        assertEquals(actual.get(PROXY_ENDPOINT).getAsString(), PROXY_ENDPOINT);
//
//        verify(rpcClient, times(1)).call(any(), any());
    }
    
    private JwtTokenVO createJwtTokenVO(String accessToken, String refreshToken) {
        JwtTokenVO jwtTokenVO = new JwtTokenVO();
        jwtTokenVO.setAccessToken(accessToken);
        jwtTokenVO.setRefreshToken(refreshToken);
        
        return jwtTokenVO;
    }

}