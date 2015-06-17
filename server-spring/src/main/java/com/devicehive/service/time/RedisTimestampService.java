package com.devicehive.service.time;

import com.devicehive.messages.bus.redis.RedisConnector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;

//@Profile({"!test"})
//@Component
//public class RedisTimestampService implements TimestampService {
//
//    @Autowired
//    private RedisConnector redisConnector;
//
//    @Override
//    public Timestamp getTimestamp() {
//        return new Timestamp(redisConnector.getTime());
//    }
//}
