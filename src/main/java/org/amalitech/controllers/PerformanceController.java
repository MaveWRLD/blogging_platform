package org.amalitech.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.amalitech.aspect.PerformanceMonitoringAspect;
import org.amalitech.dtos.perf.PerformanceStatsDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/performance")
@Tag(name = "Performance Monitoring", description = "Endpoints for monitoring application performance")
public class PerformanceController {

    private final PerformanceMonitoringAspect perfAspect;

    public PerformanceController(PerformanceMonitoringAspect perfAspect) {
        this.perfAspect = perfAspect;
    }

    @GetMapping
    @Operation(
            summary = "Get Performance Metrics",
            description = "Retrieve aggregated performance metrics for all monitored methods, including average execution time, total calls, and min/max execution times."
    )
    public PerformanceStatsDto getPerformance() {
        return perfAspect.snapshot();
    }
}
