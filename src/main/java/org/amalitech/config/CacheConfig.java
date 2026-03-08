package org.amalitech.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();

        manager.setCaffeine(defaultCacheBuilder());

        manager.registerCustomCache("postDetail",
                Caffeine.newBuilder()
                        .maximumSize(500)
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .recordStats()
                        .build());

        manager.registerCustomCache("trendingPosts",
                Caffeine.newBuilder()
                        .maximumSize(50)
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .recordStats()
                        .build());

        manager.registerCustomCache("allPosts",
                Caffeine.newBuilder()
                        .maximumSize(100)
                        .expireAfterWrite(3, TimeUnit.MINUTES)
                        .recordStats()
                        .build());

        manager.registerCustomCache("filteredPosts",
                Caffeine.newBuilder()
                        .maximumSize(200)
                        .expireAfterWrite(2, TimeUnit.MINUTES)
                        .recordStats()
                        .build());

        manager.registerCustomCache("postsByUser",
                Caffeine.newBuilder()
                        .maximumSize(300)
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .recordStats()
                        .build());

        return manager;
    }

    private Caffeine<Object, Object> defaultCacheBuilder() {
        return Caffeine.newBuilder()
                .initialCapacity(100)
                .maximumSize(1000)
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .recordStats();
    }
}