package com.devicehive.messages.bus.redis;

import com.devicehive.configuration.Constants;
import com.devicehive.exceptions.HiveException;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import redis.clients.jedis.*;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;

/**
 * Created by tmatvienko on 4/15/15.
 */
@Component
public class RedisConnector {
    private static final Logger logger = LoggerFactory.getLogger(RedisConnector.class);
    private static final Integer PAGE_SIZE = 1000;

    @Autowired
    private JedisPool jedisPool;

    public <T> SortedSet<T> fetch(String pattern, Integer count, Comparator<T> comparator, Transformer<String, T> transformer) {
        SortedSet<T> data = new TreeSet<>(comparator);
        String cursor = "0";
        do {
            ScanResult<String> keys = getKeys(cursor, pattern, PAGE_SIZE);
            cursor = keys.getStringCursor();
            for (String key : keys.getResult()) {
                T obj = transformer.apply(key);
                if (obj != null) {
                    data.add(obj);
                }
            }
            if (data.size() > count) {
                List<T> sliced = new ArrayList<>(data).subList(0, count);
                data.clear();
                data.addAll(sliced);
            }
        } while (!cursor.equals("0"));
        return data;
    }

    public ScanResult<String> getKeys(String cursor, String pattern, Integer count) {
        assert cursor != null && pattern != null && count != null;
        ScanParams params = new ScanParams()
                .count(count)
                .match(pattern);
        try (Jedis client = jedisPool.getResource()) {
            return client.scan(cursor, params);
        }
    }

    public boolean set(String sess, String key, String value, String expireSec) {
        boolean rez = false;
        Map<String, String> m = getAll(sess);
        if (m!=null) {
            m.put(key, value);
            rez = setAll(sess, m, expireSec);
        }
        return rez;
    }

    public String get(String sess, String key) {
        try (Jedis client = jedisPool.getResource()) {
            return client.hget(sess, key);
        }
    }

    public Map<String, String> getAll(String sess) {
        try (Jedis client = jedisPool.getResource()) {
            return client.hgetAll(sess);
        }
    }

    public long getTime() {
        try (Jedis client = jedisPool.getResource()) {
            List<String> times = client.time();
            if (times.size() != 2) {
                throw new IllegalArgumentException("Redis TIME returned empty list");
            }
            Long seconds = Long.parseLong(times.get(0));
            Long microseconds = Long.parseLong(times.get(1));
            return seconds * 1000 + microseconds / 1000;
        }
    }

    public boolean setAll(String sess, Map<String, String> m, String expireSec) {
        if (!NumberUtils.isNumber(expireSec)) {
            throw new HiveException(String.format("Wrong config format, should be numeric: %s", expireSec));
        }
        try (Jedis client = jedisPool.getResource()) {
            String r = client.hmset(sess, m);
            long er = client.expire(sess, Integer.parseInt(expireSec));
            return  (r.equals("OK") && er>0);
        }
    }

}
