package com.devicehive.messages.bus.redis;

import java.util.Collection;
import java.util.List;

/**
 * Created by tmatvienko on 4/15/15.
 */
public abstract class RedisService<T> {

    public abstract void save(T t);

    public abstract T getByKey(String key);

    public abstract T getByIdAndGuid(Long id, String guid);

    public abstract List<T> getByGuids(Collection<String> guid);

    public abstract List<String> getAllKeysByGuids(Collection<String> guids);

    public abstract List<String> getAllKeysByIds(Collection<String> ids);

    public abstract List<T> getAll();
}
