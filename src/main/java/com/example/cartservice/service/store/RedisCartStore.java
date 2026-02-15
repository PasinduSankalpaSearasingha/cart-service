package com.example.cartservice.service.store;

import com.example.cartservice.model.RedisCart;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name = "cart.store.type", havingValue = "redis")
public class RedisCartStore implements CartStore {

    private final Object redisTemplate; // obtained from context to avoid compile-time type issues
    private final long ttlSeconds;

    // fallback in-memory store used when Redis is unavailable
    private final ConcurrentHashMap<String, RedisCart> fallbackStore = new ConcurrentHashMap<>();

    public RedisCartStore(ApplicationContext ctx,
                          @Value("${cart.store.ttl-seconds:0}") long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
        Object bean = null;
        if (ctx.containsBean("redisTemplate")) {
            bean = ctx.getBean("redisTemplate");
        }
        this.redisTemplate = bean;
    }

    @Override
    public RedisCart load(String key) {
        if (redisTemplate == null) {
            // Redis not configured - use fallback
            return fallbackStore.get(key);
        }
        try {
            Method opsForValue = redisTemplate.getClass().getMethod("opsForValue");
            Object valueOps = opsForValue.invoke(redisTemplate);
            Method get = valueOps.getClass().getMethod("get", Object.class);
            Object v = get.invoke(valueOps, key);
            if (v == null) return null;
            if (v instanceof RedisCart) return (RedisCart) v;
            return null;
        } catch (NoSuchMethodException nsme) {
            // fallback
            return fallbackStore.get(key);
        } catch (Exception ex) {
            // fallback on any Redis access error
            return fallbackStore.get(key);
        }
    }

    @Override
    public void save(String key, RedisCart cart) {
        if (redisTemplate == null) {
            fallbackStore.put(key, cart);
            return;
        }
        try {
            Method opsForValue = redisTemplate.getClass().getMethod("opsForValue");
            Object valueOps = opsForValue.invoke(redisTemplate);
            Method set = valueOps.getClass().getMethod("set", Object.class, Object.class);
            set.invoke(valueOps, key, cart);

            if (ttlSeconds > 0) {
                try {
                    Method expire = redisTemplate.getClass().getMethod("expire", Object.class, Duration.class);
                    expire.invoke(redisTemplate, key, Duration.ofSeconds(ttlSeconds));
                } catch (NoSuchMethodException nsme) {
                    try {
                        Method expire2 = redisTemplate.getClass().getMethod("expire", Object.class, long.class, java.util.concurrent.TimeUnit.class);
                        expire2.invoke(redisTemplate, key, ttlSeconds, java.util.concurrent.TimeUnit.SECONDS);
                    } catch (NoSuchMethodException nsme2) {
                        // ignore
                    }
                }
            }
        } catch (Exception ex) {
            // fallback to in-memory on any Redis error
            fallbackStore.put(key, cart);
        }
    }

    @Override
    public void delete(String key) {
        if (redisTemplate == null) {
            fallbackStore.remove(key);
            return;
        }
        try {
            Method delete = redisTemplate.getClass().getMethod("delete", Object.class);
            delete.invoke(redisTemplate, key);
        } catch (Exception ex) {
            // fallback remove
            fallbackStore.remove(key);
        }
    }
}
