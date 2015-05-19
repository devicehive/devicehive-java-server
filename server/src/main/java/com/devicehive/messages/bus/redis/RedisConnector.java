package com.devicehive.messages.bus.redis;

import com.devicehive.configuration.Constants;
import com.devicehive.configuration.PropertiesService;
import com.devicehive.exceptions.HiveException;
import com.google.common.base.Function;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.*;
import java.util.*;

/**
 * Created by tmatvienko on 4/15/15.
 */
@Singleton
@Startup
public class RedisConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisConnector.class);
    private static final Integer PAGE_SIZE = 1000;

    private Jedis CLIENT;

    @EJB
    private PropertiesService propertiesService;

    @PostConstruct
    private void connect() {
        CLIENT = new Jedis(propertiesService.getProperty(Constants.REDDIS_CONNECTION_HOST),
                Integer.valueOf(propertiesService.getProperty(Constants.REDDIS_CONNECTION_PORT)),
                Integer.valueOf(propertiesService.getProperty(Constants.REDDIS_CONNECTION_TIMEOUT)));
        try {
            CLIENT.connect();
        } catch (Exception ex) {
            LOGGER.error("Exception occured during connecting to Redis", ex);
            ex.printStackTrace(System.err);
        }
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
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

    public long getKeysCount() {
        return CLIENT.dbSize();
    }

    public ScanResult<String> getKeys(String cursor, String pattern, Integer count) {
        assert cursor != null && pattern != null && count != null;
        ScanParams params = new ScanParams()
                .count(count)
                .match(pattern);
        return CLIENT.scan(cursor, params);
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
        return CLIENT.hget(sess, key);
    }

    public Map<String, String> getAll(String sess) {
        return CLIENT.hgetAll(sess);
    }

    public boolean setAll(String sess, Map<String, String> m, String expireSec) {
        if (!NumberUtils.isNumber(expireSec)) {
            throw new HiveException(String.format("Wrong config format, should be numeric: %s", expireSec));
        }
        String r = CLIENT.hmset(sess, m);
        long er = CLIENT.expire(sess, Integer.parseInt(expireSec));
        return  (r.equals("OK") && er>0);
    }

    public boolean isExists(String sess) {
        return CLIENT.exists(sess);
    }

    public boolean del(String sess) {
        Long del = CLIENT.del(sess);
        return del>0;
    }

    @PreDestroy
    public void close() {
        if (CLIENT.isConnected()) {
            CLIENT.disconnect();
        }
    }
}
