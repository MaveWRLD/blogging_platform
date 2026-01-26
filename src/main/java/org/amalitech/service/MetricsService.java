package org.amalitech.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

public final class MetricsService {
    
    private static final MeterRegistry registry = new SimpleMeterRegistry();
    
    private MetricsService() {}
    
    public static MeterRegistry getRegistry() {
        return registry;
    }
}
