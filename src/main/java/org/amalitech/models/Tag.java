package org.amalitech.models;


import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Tag {

    private int id;
    private String name;

    public Tag() {}

    public Tag(String name) {
        this.name = name;
    }

    public Tag(int id, String name) {
        this.id = id;
        this.name = name;
    }

}
