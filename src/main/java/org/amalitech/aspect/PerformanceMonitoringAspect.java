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

    @Pointcut("execution(* org.amalitech.service..*(..))")
    public void serviceMethods() {}

    @Pointcut("execution(* org.amalitech.algorithm..*(..))")
    public void algorithmMethods() {}

    @Pointcut("execution(* org.amalitech.controllers..*(..))")
    public void controllerMethods() {}

    @Pointcut("execution(* org.amalitech.graphqlResolver..*(..))")
    public void resolverMethods() {}

    @Around("serviceMethods() || algorithmMethods() || controllerMethods() || resolverMethods()")
    public Object monitorPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodKey = joinPoint.getSignature().toShortString();
        long startNs = System.nanoTime();

        try {
            Object result = joinPoint.proceed();
            long durationNs = System.nanoTime() - startNs;

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

            return result;

        } catch (Throwable e) {
            long durationNs = System.nanoTime() - startNs;
            double durationMs = durationNs / 1_000_000.0;

            logger.error("PERFORMANCE ERROR -> {} failed after {} ms",
                    methodKey,
                    String.format("%.3f", durationMs));

            throw e;
        }
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
