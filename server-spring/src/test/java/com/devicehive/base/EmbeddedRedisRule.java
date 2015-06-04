package com.devicehive.base;

import org.apache.commons.lang3.StringUtils;
import org.junit.rules.ExternalResource;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.util.Optional;

public class EmbeddedRedisRule extends ExternalResource {

    private RedisServer redisServer;

    public EmbeddedRedisRule() {
        String port = Optional.ofNullable(System.getProperty("redis.test.port")).flatMap(p -> {
            if (StringUtils.isBlank(p)) {
                return Optional.empty();
            }
            return Optional.of(p);
        }).orElse("6379");
        try {
            redisServer = new RedisServer(Integer.parseInt(port));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void before() throws Throwable {
        redisServer.start();
    }

    @Override
    protected void after() {
        redisServer.stop();
    }
}
