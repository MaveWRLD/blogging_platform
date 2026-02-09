package org.amalitech.dtos.perf;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PerformanceMethodMetricsDto {
    private String method;
    private long callCount;
    private double avgMs;
    private long minMs;
    private long maxMs;
}
