package com.example.cartservice.service.store;

import com.example.cartservice.model.RedisCart;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name = "cart.store.type", havingValue = "inmemory", matchIfMissing = true)
public class InMemoryCartStore implements CartStore {
    private final ConcurrentHashMap<String, RedisCart> store = new ConcurrentHashMap<>();

    @Override
    public RedisCart load(String key) {
        return store.get(key);
    }

    @Override
    public void save(String key, RedisCart cart) {
        store.put(key, cart);
    }

    @Override
    public void delete(String key) {
        store.remove(key);
    }
}
