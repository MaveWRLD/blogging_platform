package org.amalitech.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Standard success response format for successful API operations")
public class SuccessResponse<T> {
    
    @Schema(
        description = "Timestamp when the response was generated",
        example = "2024-01-01T12:00:00",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private LocalDateTime timestamp;
    
    @Schema(
        description = "Success message describing the operation result",
        example = "Operation completed successfully",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String message;
    
    @Schema(
        description = "HTTP status code",
        example = "200",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private int status;
    
    @Schema(
        description = "Response data payload",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private T data;
    
    @Schema(
        description = "Request path that was processed",
        example = "/api/posts/1"
    )
    private String path;
    
    public static <T> SuccessResponse<T> of(String message, T data, int status, String path) {
        return new SuccessResponse<>(LocalDateTime.now(), message, status, data, path);
    }
    
    public static <T> SuccessResponse<T> success(T data, String path) {
        return new SuccessResponse<>(LocalDateTime.now(), "Operation successful", 200, data, path);
    }
    
    public static <T> SuccessResponse<T> created(T data, String path) {
        return new SuccessResponse<>(LocalDateTime.now(), "Resource created successfully", 201, data, path);
    }
}
