package org.amalitech.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Validation error response for request body validation failures")
public class ValidationErrorResponse {
    
    @Schema(
        description = "Timestamp when the validation error occurred",
        example = "2024-01-01T12:00:00",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private LocalDateTime timestamp;
    
    @Schema(
        description = "General validation error message",
        example = "Validation failed",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String message;
    
    @Schema(
        description = "Field-specific validation errors",
        example = "{\"username\": \"Username is required\", \"email\": \"Invalid email format\"}",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Map<String, String> errors;
    
    @Schema(
        description = "Request path that caused the validation error",
        example = "/api/users/register"
    )
    private String path;
}
