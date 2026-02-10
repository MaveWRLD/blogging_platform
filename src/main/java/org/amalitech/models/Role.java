package org.amalitech.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Setter
@Getter
public class Role {
    private int id;
    private String name;

    public Role() {}

    public Role(String name) {}
}
