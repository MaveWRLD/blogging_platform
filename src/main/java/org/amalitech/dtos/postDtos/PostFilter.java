package org.amalitech.dtos.postDtos;

import lombok.Data;
import lombok.ToString;

import java.time.LocalDate;

@Data
@ToString
public class PostFilter {
    private int page;
    private int size;
    private String tag;
    private String author;
    private String search;
    private LocalDate fromDate;
    private LocalDate toDate;
}

