package com.shinetech.proxy.netty.common.buffer;

import com.google.common.util.concurrent.SettableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 对 response 的缓存
 */
public class RteClientResponseCache implements Serializable {


    private static final Logger logger = LoggerFactory.getLogger(RteClientResponseCache.class);

    private final ConcurrentHashMap<String, SettableFuture<Object>> cache = new ConcurrentHashMap<>();


    private RteClientResponseCache() {
    }


    public static RteClientResponseCache newBuild() {
        return new RteClientResponseCache();
    }

    public void createStub(String serialKey) {
        //初始化 key

        cache.put(serialKey, SettableFuture.create());


    }


    // 把结果放到缓存
    public void putResult(String serialKey, Object body) {

        SettableFuture<Object> future = cache.get(serialKey);
        if (future == null) {
            logger.error("serialkey cache is null, key: " + serialKey + ", body: " + body + " cachesize: " + cache.size());
        } else {
            future.set(body);
        }
    }

    public long size() {
        return cache.size();
    }

    public Map map() {
        return cache;
    }

    /**
     * 根据key值拿到结果
     *
     * @param serialKey key
     * @param timeout   超时时间,毫秒
     * @return
     */
    public Object getResult(String serialKey, long timeout) {
        SettableFuture<Object> future;
        try {
            future = cache.get(serialKey);
            if (future == null) {
                logger.error("serialkey in the memory is null: " + serialKey);
            } else {
                Object resp = future.get(timeout, TimeUnit.MILLISECONDS);
                cache.remove(serialKey);
                return resp;
            }
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        } catch (ExecutionException e) {
            logger.error(e.getMessage(), e);
        } catch (TimeoutException e) {
            logger.error(e.getMessage() + " , Exception: " + e.getClass().getName() + ", serialkey: " + serialKey);
        }
        return null;
    }

}
