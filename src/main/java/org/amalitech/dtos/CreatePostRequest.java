package org.amalitech.dtos;


import lombok.Data;


@Data
public class CreatePostRequest {

    private String title;
    private String body;

}
