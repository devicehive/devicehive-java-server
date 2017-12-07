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

import com.devicehive.proxy.PluginProxyClient;
import com.devicehive.service.helpers.HttpRestHelper;
import com.devicehive.vo.PluginVO;
import com.google.gson.JsonObject;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.ServiceUnavailableException;
import java.util.Arrays;

import static com.devicehive.model.enums.PluginStatus.ACTIVE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class PluginHealthCheckServiceTest {
    private static final String HEALTH_CHECK_URL = "healthCheckUrl"; 
    
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private PluginService pluginService;
    @Mock
    private PluginProxyClient rpcClient;
    @Mock
    private HttpRestHelper httpRestHelper;
    
    @InjectMocks
    private PluginHealthCheckService pluginHealthCheckService;
    
    @Test
    public void shouldPerformHealthCheckWhenServiceIsResponding() throws Exception {
        //given
        given(pluginService.findByStatus(eq(ACTIVE))).willReturn(Arrays.asList(createPluginVO(HEALTH_CHECK_URL)));
        given(httpRestHelper.get(eq(HEALTH_CHECK_URL), eq(JsonObject.class), eq(null))).willReturn(new JsonObject());
        
        //when
        pluginHealthCheckService.performHealthCheck();
        
        //then
        verify(rpcClient, times(0)).call(any(), any());
    }

    @Test
    public void shouldPerformHealthCheckWhenServiceIsNotResponding() throws Exception {
        //given
        given(pluginService.findByStatus(eq(ACTIVE))).willReturn(Arrays.asList(createPluginVO(HEALTH_CHECK_URL)));
        given(httpRestHelper.get(eq(HEALTH_CHECK_URL), eq(JsonObject.class), eq(null)))
                .willThrow(new ServiceUnavailableException());

        //when
        pluginHealthCheckService.performHealthCheck();

        //then
        verify(rpcClient, times(1)).call(any(), any());
    }

    private PluginVO createPluginVO(String healthCheckUrl) {
        PluginVO pluginVO = new PluginVO();
        pluginVO.setStatus(ACTIVE);
        pluginVO.setHealthCheckUrl(healthCheckUrl);

        return pluginVO;
    }

}