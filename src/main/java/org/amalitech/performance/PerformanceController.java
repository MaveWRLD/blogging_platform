package org.amalitech.performance;

import org.amalitech.performance.api.PerformanceApi;
import org.amalitech.performance.PerformanceMonitoringAspect;
import org.amalitech.performance.dto.PerformanceStatsDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/performance")
public class PerformanceController implements PerformanceApi {

    private final PerformanceMonitoringAspect perfAspect;

    public PerformanceController(PerformanceMonitoringAspect perfAspect) {
        this.perfAspect = perfAspect;
    }

    @Override
    @GetMapping
    public PerformanceStatsDto getPerformance() {
        return perfAspect.snapshot();
    }
}
