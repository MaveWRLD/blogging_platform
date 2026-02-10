package org.amalitech.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import org.amalitech.dtos.perf.PerformanceMethodMetricsDto;
import org.amalitech.dtos.perf.PerformanceStatsDto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Aspect
@Component
public class PerformanceMonitoringAspect {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitoringAspect.class);

    private final ConcurrentHashMap<String, MethodMetrics> metricsMap = new ConcurrentHashMap<>();

    private static class MethodMetrics {
        AtomicLong totalExecutionTime = new AtomicLong(0);
        AtomicLong callCount = new AtomicLong(0);
        AtomicLong minTime = new AtomicLong(Long.MAX_VALUE);
        AtomicLong maxTime = new AtomicLong(0);

        void addExecution(long duration) {
            totalExecutionTime.addAndGet(duration);
            callCount.incrementAndGet();

            long currentMin = minTime.get();
            while (duration < currentMin && !minTime.compareAndSet(currentMin, duration)) {
                currentMin = minTime.get();
            }

            long currentMax = maxTime.get();
            while (duration > currentMax && !maxTime.compareAndSet(currentMax, duration)) {
                currentMax = maxTime.get();
            }
        }

        double getAverageTime() {
            long count = callCount.get();
            return count > 0 ? (double) totalExecutionTime.get() / count : 0;
        }
    }

    @Pointcut("execution(* org.amalitech.service..*(..))")
    public void serviceMethods() {}

    @Pointcut("execution(* org.amalitech.algorithm..*(..))")
    public void algorithmMethods() {}

    /**
     * Log all controller methods
     */
    @Pointcut("execution(* org.amalitech.controllers..*(..))")
    public void controllerMethods() {}

    @Pointcut("execution(* org.amalitech.graphqlResolver..*(..))")
    public void resolverMethods() {}


    /**
     * Monitor performance of service and algorithm methods
     */
    @Around("serviceMethods() || algorithmMethods() || controllerMethods() || resolverMethods()")
    public Object monitorPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodKey = joinPoint.getSignature().toShortString();
        long startTime = System.nanoTime();

        try {
            Object result = joinPoint.proceed();
            long duration = (System.nanoTime() - startTime) / 1_000_000;

            MethodMetrics metrics = metricsMap.computeIfAbsent(methodKey, k -> new MethodMetrics());
            metrics.addExecution(duration);

            logger.debug("PERFORMANCE -> {} executed in {}ms", methodKey, duration);

            if (duration > 500) {
                logger.warn("SLOW EXECUTION -> {} took {}ms (avg: {}ms)",
                        methodKey, duration, String.format("%.2f", metrics.getAverageTime()));
            }

            return result;

        } catch (Throwable e) {
            long duration = (System.nanoTime() - startTime) / 1_000_000;
            logger.error("PERFORMANCE ERROR -> {} failed after {}ms", methodKey, duration);
            throw e;
        }
    }

    private MethodMetrics getMetrics(String methodKey) {
        return metricsMap.get(methodKey);
    }

    /**
     * Snapshot current performance metrics as a DTO
     */
    public PerformanceStatsDto snapshot() {
        List<PerformanceMethodMetricsDto> list = new ArrayList<>();
        metricsMap.forEach((method, metrics) -> {
            long callCount = metrics.callCount.get();
            double avg = metrics.getAverageTime();
            long min = metrics.minTime.get();
            if (min == Long.MAX_VALUE) min = 0;
            long max = metrics.maxTime.get();

            list.add(new PerformanceMethodMetricsDto(method, callCount, avg, min, max));
        });
        return new PerformanceStatsDto(Instant.now(), list);
    }

    /**
     * Print all performance statistics
     */
    public void printStatistics() {
        logger.info("=== PERFORMANCE STATISTICS ===");
        metricsMap.forEach((method, metrics) -> {
            logger.info("Method: {}", method);
            logger.info("  Calls: {}", metrics.callCount.get());
            logger.info("  Avg Time: {}ms", String.format("%.2f", metrics.getAverageTime()));            logger.info("  Min Time: {}ms", metrics.minTime.get());
            logger.info("  Max Time: {}ms", metrics.maxTime.get());
        });
    }
}