package org.amalitech.dtos.postDtos;

import lombok.Data;

import java.time.LocalDate;

@Data
public class PostFilterInput {
    private int page = 0;
    private int size = 12;
    private String tag;
    private String author;
    private String search;
    private LocalDate fromDate;
    private LocalDate toDate;
}
