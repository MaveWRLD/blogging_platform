package org.amalitech.dtos.postDtos;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

@Data
public class UpdatePostRequest {
    private String title;
    private String body;
    @Pattern(regexp = "^(draft|published|)$", message = "Status must be one of: draft, published")
    private String status;
    private List<Integer> tagIds;
}
