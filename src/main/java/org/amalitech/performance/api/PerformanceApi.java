package org.amalitech.performance.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.amalitech.performance.dto.PerformanceStatsDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/performance")
@Tag(name = "Performance Monitoring", description = "Endpoints for monitoring application performance")
@SecurityRequirement(name = "bearerAuth")
public interface PerformanceApi {

    @GetMapping
    @PreAuthorize("hasRole('admin')")
    @Operation(
            summary = "Get Performance Metrics",
            description = "Retrieve aggregated performance metrics for all monitored methods, including average execution time, total calls, and min/max execution times. Requires admin privileges."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Performance metrics retrieved successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = PerformanceStatsDto.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication required",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"error\":\"Unauthorized\",\"message\":\"Full authentication is required to access this resource\",\"path\":\"/performance\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Admin role required",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"error\":\"Forbidden\",\"message\":\"Admin role required to access performance metrics\",\"path\":\"/performance\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error - Failed to retrieve performance data",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Failed to retrieve performance metrics\",\"path\":\"/performance\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "503",
                    description = "Service Unavailable - Performance monitoring service is temporarily unavailable",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Performance monitoring service unavailable\",\"path\":\"/performance\"}"))
            )
    })
    PerformanceStatsDto getPerformance();
}
