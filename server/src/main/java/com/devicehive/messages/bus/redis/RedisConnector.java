package com.devicehive.messages.bus.redis;

import com.devicehive.configuration.Constants;
import com.devicehive.configuration.PropertiesService;
import com.devicehive.exceptions.HiveException;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import java.util.Map;
import java.util.Set;

/**
 * Created by tmatvienko on 4/15/15.
 */
@Singleton
@Startup
public class RedisConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisConnector.class);
    private Jedis CLIENT;

    @EJB
    private PropertiesService propertiesService;

    @PostConstruct
    private void connect() {
        CLIENT = new Jedis(propertiesService.getProperty(Constants.REDDIS_CONNECTION_HOST));
        try {
            CLIENT.connect();
        } catch (Exception ex) {
            LOGGER.error("Exception occured during connecting to Redis", ex);
            ex.printStackTrace(System.err);
        }
    }

    public long getKeysCount() {
        return CLIENT.dbSize();
    }

    public Set<String> getAllKeys(String pattern) {
        return CLIENT.keys(pattern);
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
