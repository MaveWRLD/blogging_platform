package org.amalitech.performance;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import org.amalitech.performance.dto.PerformanceMethodMetricsDto;
import org.amalitech.performance.dto.PerformanceStatsDto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Aspect
@Component
public class PerformanceMonitoringAspect {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitoringAspect.class);

    private final ConcurrentHashMap<String, MethodMetrics> metricsMap = new ConcurrentHashMap<>();

    private static class MethodMetrics {
        AtomicLong totalExecutionTimeNs = new AtomicLong(0);
        AtomicLong callCount = new AtomicLong(0);
        AtomicLong minTimeNs = new AtomicLong(Long.MAX_VALUE);
        AtomicLong maxTimeNs = new AtomicLong(0);

        void addExecution(long durationNs) {
            totalExecutionTimeNs.addAndGet(durationNs);
            callCount.incrementAndGet();

            long currentMin = minTimeNs.get();
            while (durationNs < currentMin && !minTimeNs.compareAndSet(currentMin, durationNs)) {
                currentMin = minTimeNs.get();
            }

            long currentMax = maxTimeNs.get();
            while (durationNs > currentMax && !maxTimeNs.compareAndSet(currentMax, durationNs)) {
                currentMax = maxTimeNs.get();
            }
        }

        double getAverageMs() {
            long count = callCount.get();
            return count > 0
                    ? (totalExecutionTimeNs.get() / 1_000_000.0) / count
                    : 0;
        }

        double getMinMs() {
            long min = minTimeNs.get();
            if (min == Long.MAX_VALUE) return 0;
            return min / 1_000_000.0;
        }

        double getMaxMs() {
            return maxTimeNs.get() / 1_000_000.0;
        }
    }

    // Package-by-feature moved Service/Controller/Resolver classes out of the
    // old service../controllers../graphqlResolver.. packages into per-feature
    // packages (post., comment., user., auth., ...), so these pointcuts match
    // by class name suffix instead of by package.
    @Pointcut("execution(* org.amalitech..*Service.*(..))")
    public void serviceMethods() {}

    @Pointcut("execution(* org.amalitech..algorithm..*(..))")
    public void algorithmMethods() {}

    @Pointcut("execution(* org.amalitech..*Controller.*(..))")
    public void controllerMethods() {}

    @Pointcut("execution(* org.amalitech..*Resolver.*(..))")
    public void resolverMethods() {}

    @Around("serviceMethods() || algorithmMethods() || controllerMethods() || resolverMethods()")
    public Object monitorPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodKey = joinPoint.getSignature().toShortString();
        long startNs = System.nanoTime();

        try {
            Object result = joinPoint.proceed();
            long durationNs = System.nanoTime() - startNs;

            recordMetricsAsync(methodKey, durationNs);

            return result;

        } catch (Throwable e) {
            long durationNs = System.nanoTime() - startNs;

            recordErrorAsync(methodKey, durationNs);

            throw e;
        }
    }

    /**
     * Asynchronously records successful method execution metrics.
     * Uses CompletableFuture to allow the main thread to continue without waiting.
     *
     * @param methodKey the method identifier
     * @param durationNs the execution time in nanoseconds
     */
    @Async("performanceExecutor")
    public CompletableFuture<Void> recordMetricsAsync(String methodKey, long durationNs) {
        return CompletableFuture.runAsync(() -> {
            MethodMetrics metrics = metricsMap.computeIfAbsent(methodKey, k -> new MethodMetrics());
            metrics.addExecution(durationNs);

            double durationMs = durationNs / 1_000_000.0;

            logger.debug("PERFORMANCE -> {} executed in {} ms", methodKey, String.format("%.3f", durationMs));

            if (durationMs > 500) {
                logger.warn("SLOW EXECUTION -> {} took {} ms (avg: {} ms)",
                        methodKey,
                        String.format("%.3f", durationMs),
                        String.format("%.3f", metrics.getAverageMs()));
            }
        });
    }

    /**
     * Asynchronously records method execution errors.
     * Uses CompletableFuture to allow the main thread to continue without waiting.
     *
     * @param methodKey the method identifier
     * @param durationNs the execution time before failure in nanoseconds
     */
    @Async("performanceExecutor")
    public CompletableFuture<Void> recordErrorAsync(String methodKey, long durationNs) {
        return CompletableFuture.runAsync(() -> {
            double durationMs = durationNs / 1_000_000.0;

            logger.error("PERFORMANCE ERROR -> {} failed after {} ms",
                    methodKey,
                    String.format("%.3f", durationMs));
        });
    }

    public PerformanceStatsDto snapshot() {
        List<PerformanceMethodMetricsDto> list = new ArrayList<>();

        metricsMap.forEach((method, metrics) -> {
            long callCount = metrics.callCount.get();

            list.add(new PerformanceMethodMetricsDto(
                    method,
                    callCount,
                    metrics.getAverageMs(),
                    metrics.getMinMs(),
                    metrics.getMaxMs()
            ));
        });

        return new PerformanceStatsDto(Instant.now(), list);
    }
}
