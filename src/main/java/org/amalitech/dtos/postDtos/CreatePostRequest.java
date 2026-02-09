package org.amalitech.dtos.postDtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreatePostRequest {
    @NotBlank(message = "Title cannot be empty")
    private String title;

    @NotBlank(message = "Body cannot be empty")
    private String body;

    @NotNull(message = "User ID cannot be empty")
    @JsonProperty("user_id")
    private int userId;

    private String status;
}