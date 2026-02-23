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
@Schema(description = "Standard error response format for API errors")
public class ErrorResponse {
    
    @Schema(
        description = "Timestamp when the error occurred",
        example = "2024-01-01T12:00:00",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private LocalDateTime timestamp;
    
    @Schema(
        description = "Error message describing what went wrong",
        example = "Resource not found",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String message;
    
    @Schema(
        description = "HTTP status code",
        example = "404",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private int status;
    
    @Schema(
        description = "Error type or category",
        example = "NOT_FOUND"
    )
    private String error;
    
    @Schema(
        description = "Request path that caused the error",
        example = "/api/posts/1"
    )
    private String path;
    
    @Schema(
        description = "Validation errors for request body fields (present only for validation failures)",
        example = "{\"title\": \"Title is required\", \"body\": \"Body must not be blank\"}"
    )
    private Map<String, String> errors;
    
    @Schema(
        description = "Additional error details or stack trace (in development mode only)",
        example = "org.amalitech.exception.ResourceNotFoundException: Post not found"
    )
    private String details;
}
