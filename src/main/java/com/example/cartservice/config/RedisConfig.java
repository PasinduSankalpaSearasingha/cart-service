package com.example.cartservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

@Configuration
@ConditionalOnProperty(name = "cart.store.type", havingValue = "redis")
public class RedisConfig {

    @Bean
    public Object redisConnectionFactory(
            @Value("${spring.redis.host:localhost}") String host,
            @Value("${spring.redis.port:6379}") int port) {
        try {
            // Class name from Spring Data Redis
            Class<?> lettuceClass = Class.forName("org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory");
            Constructor<?> ctor = null;
            try {
                ctor = lettuceClass.getConstructor(String.class, int.class);
                return ctor.newInstance(host, port);
            } catch (NoSuchMethodException nsme) {
                // try no-arg constructor and set properties via setters
                Constructor<?> noArg = lettuceClass.getConstructor();
                Object factory = noArg.newInstance();
                try {
                    Method setHost = lettuceClass.getMethod("setHostName", String.class);
                    Method setPort = lettuceClass.getMethod("setPort", int.class);
                    setHost.invoke(factory, host);
                    setPort.invoke(factory, port);
                } catch (NoSuchMethodException inner) {
                    // ignore; best-effort
                }
                return factory;
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Spring Data Redis classes not found on classpath. Add 'spring-boot-starter-data-redis' or set cart.store.type!=redis", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create LettuceConnectionFactory via reflection", e);
        }
    }

    @Bean
    public Object redisTemplate(Object redisConnectionFactory) {
        try {
            Class<?> redisTemplateClass = Class.forName("org.springframework.data.redis.core.RedisTemplate");
            Class<?> redisSerializerClass = Class.forName("org.springframework.data.redis.serializer.RedisSerializer");
            Class<?> stringSerializerClass = Class.forName("org.springframework.data.redis.serializer.StringRedisSerializer");
            Class<?> jsonSerializerClass = Class.forName("org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer");

            Object template = redisTemplateClass.getConstructor().newInstance();

            // setConnectionFactory
            try {
                Method setCF = redisTemplateClass.getMethod("setConnectionFactory", Class.forName("org.springframework.data.redis.connection.RedisConnectionFactory"));
                setCF.invoke(template, redisConnectionFactory);
            } catch (NoSuchMethodException nsme) {
                // ignore
            }

            // setKeySerializer
            Object keySer = stringSerializerClass.getConstructor().newInstance();
            try {
                Method setKey = redisTemplateClass.getMethod("setKeySerializer", redisSerializerClass);
                setKey.invoke(template, keySer);
            } catch (NoSuchMethodException ignored) {}

            // setValueSerializer
            Object valSer = jsonSerializerClass.getConstructor().newInstance();
            try {
                Method setVal = redisTemplateClass.getMethod("setValueSerializer", redisSerializerClass);
                setVal.invoke(template, valSer);
            } catch (NoSuchMethodException ignored) {}

            // afterPropertiesSet
            try {
                Method after = redisTemplateClass.getMethod("afterPropertiesSet");
                after.invoke(template);
            } catch (NoSuchMethodException ignored) {}

            return template;
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Spring Data Redis classes not found on classpath. Add 'spring-boot-starter-data-redis' or set cart.store.type!=redis", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create RedisTemplate via reflection", e);
        }
    }
}
