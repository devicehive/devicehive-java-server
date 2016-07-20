package com.devicehive.dao.riak;

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.commands.kv.DeleteValue;
import com.basho.riak.client.api.commands.kv.StoreValue;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import com.basho.riak.client.core.util.BinaryValue;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Aspect
@Component
@Profile({"riak"})
public class RiakCleanupHelper {

    private List<Location> storedObjectLocations = new ArrayList<>();

    @Autowired
    private RiakClient client;

    @Pointcut("execution(public * com.basho.riak.client.api.RiakClient.execute(..))")
    private void collectObjectLocations() {

    }

    @Around("collectObjectLocations()")
    public Object doBasicProfiling(ProceedingJoinPoint pjp) throws Throwable {
        // start stopwatch
        if (pjp.getArgs() != null && pjp.getArgs().length > 0 && pjp.getArgs()[0] instanceof StoreValue) {
            StoreValue storeValue = (StoreValue) pjp.getArgs()[0];
            Field namespaceField = storeValue.getClass().getDeclaredField("namespace"); //NoSuchFieldException
            namespaceField.setAccessible(true);
            Namespace namespace = (Namespace) namespaceField.get(storeValue); //IllegalAccessException
            Field keyField = storeValue.getClass().getDeclaredField("key"); //NoSuchFieldException
            keyField.setAccessible(true);
            BinaryValue key = (BinaryValue) keyField.get(storeValue);
            Location location = new Location(namespace, key);
            storedObjectLocations.add(location);
        }
        // stop stopwatch
        return pjp.proceed();
    }

    public void cleanupLocations() throws ExecutionException, InterruptedException {
        for (Location location : storedObjectLocations) {
            DeleteValue deleteValue = new DeleteValue.Builder(location).build();
            client.execute(deleteValue);
        }
    }
}
