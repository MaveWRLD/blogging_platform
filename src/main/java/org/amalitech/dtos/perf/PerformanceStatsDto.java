package org.amalitech.dtos.perf;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@AllArgsConstructor
public class PerformanceStatsDto {
    private Instant timestamp;
    private List<PerformanceMethodMetricsDto> metrics;
}
