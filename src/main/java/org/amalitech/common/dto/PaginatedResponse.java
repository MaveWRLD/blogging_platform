package org.amalitech.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Standard paginated response format for list endpoints")
public class PaginatedResponse<T> {
    
    @Schema(
        description = "Timestamp when the response was generated",
        example = "2024-01-01T12:00:00"
    )
    private LocalDateTime timestamp;
    
    @Schema(
        description = "Response message",
        example = "Data retrieved successfully"
    )
    private String message;
    
    @Schema(
        description = "HTTP status code",
        example = "200"
    )
    private int status;
    
    @Schema(
        description = "List of data items for the current page",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private List<T> content;
    
    @Schema(
        description = "Current page number (0-based)",
        example = "0",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private int page;
    
    @Schema(
        description = "Number of items per page",
        example = "10",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private int size;
    
    @Schema(
        description = "Total number of items across all pages",
        example = "100",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private long totalElements;
    
    @Schema(
        description = "Total number of pages",
        example = "10",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private int totalPages;
    
    @Schema(
        description = "Whether there is a previous page",
        example = "false",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private boolean hasPrevious;
    
    @Schema(
        description = "Whether there is a next page",
        example = "true",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private boolean hasNext;
    
    @Schema(
        description = "Whether the current page is the first page",
        example = "true",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private boolean first;
    
    @Schema(
        description = "Whether the current page is the last page",
        example = "false",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private boolean last;
    
    public static <T> PaginatedResponse<T> of(
            List<T> content, 
            int page, 
            int size, 
            long totalElements, 
            int totalPages, 
            boolean hasPrevious, 
            boolean hasNext,
            String message
    ) {
        PaginatedResponse<T> response = new PaginatedResponse<>();
        response.timestamp = LocalDateTime.now();
        response.message = message;
        response.status = 200;
        response.content = content;
        response.page = page;
        response.size = size;
        response.totalElements = totalElements;
        response.totalPages = totalPages;
        response.hasPrevious = hasPrevious;
        response.hasNext = hasNext;
        response.first = page == 0;
        response.last = page == totalPages - 1;
        return response;
    }
}
