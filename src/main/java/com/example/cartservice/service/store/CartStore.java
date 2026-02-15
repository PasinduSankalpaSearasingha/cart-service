package com.example.cartservice.service.store;

import com.example.cartservice.model.RedisCart;

public interface CartStore {
    RedisCart load(String key);
    void save(String key, RedisCart cart);
    void delete(String key);
}

