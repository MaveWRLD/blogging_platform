package org.amalitech.dtos.postDtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;

@Data
public class CreatePostRequest {
    @NotBlank(message = "Title cannot be empty")
    private String title;

    @NotBlank(message = "Body cannot be empty")
    private String body;

    private Set<String> tagNames;

    private String status;
}