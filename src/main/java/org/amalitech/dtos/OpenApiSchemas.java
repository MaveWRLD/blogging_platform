package org.amalitech.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Common OpenAPI schemas for consistent API documentation
 * This class provides reusable schema definitions for API responses
 */
public class OpenApiSchemas {

    @Schema(description = "Standard error response format")
    public static class ErrorResponseSchema extends ErrorResponse {}

    @Schema(description = "Validation error response format")
    public static class ValidationErrorResponseSchema extends ValidationErrorResponse {}

    @Schema(description = "Standard success response format")
    public static class SuccessResponseSchema<T> extends SuccessResponse<T> {}

    @Schema(description = "Paginated response format")
    public static class PaginatedResponseSchema<T> extends PaginatedResponse<T> {}

    @Schema(
        example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Resource not found\",\"status\":404,\"error\":\"NOT_FOUND\",\"path\":\"/api/posts/1\"}"
    )
    public static class NotFoundExample {}

    @Schema(
        example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"errors\":{\"username\":\"Username is required\",\"email\":\"Invalid email format\"},\"message\":\"Validation failed\",\"path\":\"/api/users/register\"}"
    )
    public static class ValidationErrorExample {}

    @Schema(
        example = "{\"error\":\"Unauthorized\",\"message\":\"Full authentication is required to access this resource\",\"path\":\"/api/posts\"}"
    )
    public static class UnauthorizedExample {}

    @Schema(
        example = "{\"error\":\"Forbidden\",\"message\":\"Access is denied\",\"path\":\"/api/users\"}"
    )
    public static class ForbiddenExample {}

    @Schema(
        example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Internal server error\",\"status\":500,\"path\":\"/api/posts\"}"
    )
    public static class InternalServerErrorExample {}
}
