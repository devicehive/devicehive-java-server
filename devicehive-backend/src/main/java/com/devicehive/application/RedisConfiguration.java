package com.devicehive.application;

/*
 * #%L
 * DeviceHive Java Server Common business logic
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

import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.hash.HashMapper;
import org.springframework.data.redis.hash.Jackson2HashMapper;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfiguration {

    @Bean
    public RedisTemplate<String, DeviceNotification> deviceNotificationRedisTemplate(
            final RedisConnectionFactory redisConnectionFactory,
            final RedisSerializer<String> stringRedisSerializer,
            final RedisSerializer<DeviceNotification> deviceNotificationRedisSerializer) {

        RedisTemplate<String, DeviceNotification> template = new RedisTemplate<>();

        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(stringRedisSerializer);
        template.setValueSerializer(deviceNotificationRedisSerializer);

        return template;
    }

    @Bean
    public RedisTemplate<String, DeviceCommand> deviceCommandRedisTemplate(
            final RedisConnectionFactory redisConnectionFactory,
            final RedisSerializer<String> stringRedisSerializer,
            final RedisSerializer<DeviceCommand> deviceCommandRedisSerializer) {

        RedisTemplate<String, DeviceCommand> template = new RedisTemplate<>();

        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(stringRedisSerializer);
        template.setValueSerializer(deviceCommandRedisSerializer);

        return template;
    }

    @Bean
    public RedisSerializer<String> stringRedisSerializer() {
        return new StringRedisSerializer();
    }

    @Bean
    public RedisSerializer<DeviceNotification> deviceNotificationRedisSerializer() {
        return new Jackson2JsonRedisSerializer<>(DeviceNotification.class);
    }

    @Bean
    public RedisSerializer<DeviceCommand> deviceCommandRedisSerializer() {
        return new Jackson2JsonRedisSerializer<>(DeviceCommand.class);
    }

    @Bean
    public HashMapper<Object, String, Object> hashMapper() {
        return new Jackson2HashMapper(false);
    }
}
