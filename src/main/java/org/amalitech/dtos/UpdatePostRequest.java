package org.amalitech.dtos;

import lombok.Data;

@Data
public class UpdatePostRequest {
    private String title;
    private String body;
}
