package org.amalitech.util.RowMappers;

import org.amalitech.models.Comment;
import org.bson.Document;

public class CommentRowMapper {
    public CommentRowMapper() {
    }

    /**
     * Map MongoDB Document to Comment object.
     *
     * @param doc the MongoDB document
     * @return the Comment object
     */
    public static Comment mapToComment(Document doc) {
        Comment comment = new Comment();
        comment.setId(doc.getObjectId("_id").toHexString());
        comment.setPostId(doc.getInteger("postId"));
        comment.setUsername(doc.getString("userName"));
        comment.setBody(doc.getString("body"));
        comment.setCreatedAt(doc.getDate("createdAt").toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
        comment.setParentCommentId(doc.getInteger("parentCommentId"));

        return comment;
    }
}