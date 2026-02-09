package org.amalitech.controllers;

import org.amalitech.aspect.PerformanceMonitoringAspect;
import org.amalitech.dtos.perf.PerformanceStatsDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/performance")
public class PerformanceController {

    private final PerformanceMonitoringAspect perfAspect;

    public PerformanceController(PerformanceMonitoringAspect perfAspect) {
        this.perfAspect = perfAspect;
    }

    @GetMapping
    public PerformanceStatsDto getPerformance() {
        return perfAspect.snapshot();
    }
}
