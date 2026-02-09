package org.amalitech.aspect;

import org.amalitech.algorithm.CacheManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
public class CachingAspect {

    private static final Logger logger = LoggerFactory.getLogger(CachingAspect.class);

    @Autowired
    private CacheManager cacheManager;

    /**
     * Intercept methods annotated with @Cacheable
     */
    @Around("@annotation(cacheable)")
    public Object cacheMethodResult(ProceedingJoinPoint joinPoint, Cacheable cacheable) throws Throwable {

        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        String cacheKey = buildCacheKey(cacheable.keyPrefix(), methodName, args);

        Object cachedResult = cacheManager.get(cacheKey);
        if (cachedResult != null) {
            logger.debug("CACHE HIT -> {}", cacheKey);
            return cachedResult;
        }

        logger.debug("CACHE MISS -> {}", cacheKey);

        Object result = joinPoint.proceed();

        if (result != null) {
            cacheManager.put(cacheKey, result, cacheable.ttlSeconds());
            logger.debug("CACHE STORED -> {} (TTL: {}s)", cacheKey, cacheable.ttlSeconds());
        }

        return result;
    }

    private String buildCacheKey(String prefix, String methodName, Object[] args) {
        StringBuilder key = new StringBuilder();

        if (prefix != null && !prefix.isEmpty()) {
            key.append(prefix).append(":");
        }

        key.append(methodName);

        if (args != null && args.length > 0) {
            key.append(":").append(Arrays.hashCode(args));
        }

        return key.toString();
    }
}