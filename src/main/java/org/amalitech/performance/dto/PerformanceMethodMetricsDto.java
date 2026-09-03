package org.amalitech.performance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PerformanceMethodMetricsDto {

    private String method;
    private long callCount;
    private double avgMs;
    private double minMs;
    private double maxMs;

}

