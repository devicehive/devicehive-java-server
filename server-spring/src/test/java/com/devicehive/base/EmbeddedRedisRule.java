package com.devicehive.base;

import org.junit.rules.ExternalResource;
import redis.embedded.RedisServer;

import java.io.IOException;

public class EmbeddedRedisRule extends ExternalResource {

    private RedisServer redisServer;

    public EmbeddedRedisRule() {
        try {
            redisServer = new RedisServer(6379);
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
