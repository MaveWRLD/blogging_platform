package org.amalitech.dtos.postDtos;

import lombok.Data;
import lombok.ToString;

import java.time.Instant;

@Data
@ToString
public class PostFilter {
    private int page;
    private int size;
    private String title;
    private String tag;
    private String author;
    private String search;
    private Instant fromDate;
    private Instant toDate;
}

