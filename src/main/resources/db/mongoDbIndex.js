private Post mapRowToPost(ResultSet rs) throws SQLException {
        Post post = new Post(
                rs.getInt("id"),
                rs.getString("title"),
                rs.getString("body"),
                rs.getInt("user_id"),
                rs.getString("status"));

        java.sql.Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            post.setCreatedAt(createdAt.toLocalDateTime());
        }

        java.sql.Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            post.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return post;
    }


    private Tag mapRowToTag(ResultSet rs) throws SQLException {
        return new Tag(rs.getInt("id"), rs.getString("name"));
    }

    private User mapRowToUser(ResultSet rs) throws SQLException {
        User user = new User(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("email"),
                rs.getString("password"),
                rs.getString("role"),
                rs.getString("status")
        );

        java.sql.Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            user.setCreatedAt(ts.toLocalDateTime());
        }
        return user;
    }

    private Comment mapToComment(Document doc) {
        Comment comment = new Comment();
        comment.setId(doc.getObjectId("_id").toHexString());
        comment.setPostId(doc.getInteger("postId"));
        comment.setUserName(doc.getString("userName"));
        comment.setBody(doc.getString("body"));
        comment.setParentCommentId(doc.getInteger("parentCommentId"));

        return comment;
    }