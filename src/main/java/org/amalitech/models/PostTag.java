package org.amalitech.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Setter
@Getter
public class PostTag {

    private int postId;
    private int tagId;

    public PostTag() {}

    @Override
    public String toString() {
        return "PostTag{" +
                "postId=" + postId +
                ", tagId=" + tagId +
                '}';
    }
}
