package org.amalitech.dtos.postDtos;

import lombok.Data;

import java.util.List;

@Data
public class UpdatePostRequest {
    private String title;
    private String body;
    private String status;
    private List<Integer> tagIds;
}
