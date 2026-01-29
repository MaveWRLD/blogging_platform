package org.amalitech.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.amalitech.config.MongoConnectionProvider;
import org.amalitech.interfaces.CommentRepository;
import org.amalitech.util.exception.DatabaseException;
import org.amalitech.models.Comment;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * MongoDB implementation of CommentRepository.
 * Manages comment persistence in MongoDB using the MongoDB connection provider.
 */
public class MongoCommentDao implements CommentRepository {

    private final MongoCollection<Document> commentsCollection;

    /**
     * Constructor that gets the comments collection from MongoDB connection provider.
     */
    public MongoCommentDao() {
        this.commentsCollection = MongoConnectionProvider.getCommentsCollection();
    }

    /**
     * @param commentsCollection MongoDB collection for comments
     */
    public MongoCommentDao(MongoCollection<Document> commentsCollection) {
        this.commentsCollection = commentsCollection;
    }

    @Override
    public void save(Comment comment) {
        Document doc = new Document()
                .append("postId", comment.getPostId())
                .append("userName", comment.getUserName())
                .append("body", comment.getBody())
                .append("parentCommentId", comment.getParentCommentId())
                .append("createdAt", LocalDateTime.now())
                .append("updatedAt", LocalDateTime.now());
        commentsCollection.insertOne(doc);
        comment.setId(doc.getObjectId("_id").toHexString());
    }

    @Override
    public Comment findByObjectId(String objectId) {
        try {
            Document doc = commentsCollection.find(Filters.eq("_id", new ObjectId(objectId))).first();
            if (doc == null) {
                throw new DatabaseException("Comment with ID " + objectId + " not found");
            }
            return mapToComment(doc);
        } catch (IllegalArgumentException e) {
            throw new DatabaseException("Invalid comment ID format: " + objectId);
        }
    }

    @Override
    public List<Comment> findByPostId(int postId) {
        List<Comment> comments = new ArrayList<>();
        commentsCollection.find(Filters.eq("postId", postId)).forEach(doc -> comments.add(mapToComment(doc)));
        return comments;
    }

    @Override
    public void update(Comment comment) {
        try {
            commentsCollection.updateOne(
                    Filters.eq("_id", new ObjectId(comment.getId())),
                    Updates.combine(
                            Updates.set("body", comment.getBody()),
                            Updates.set("updatedAt", LocalDateTime.now())
                    )
            );
        } catch (IllegalArgumentException e) {
            throw new DatabaseException("Invalid comment ID format: " + comment.getId(), e);
        }
    }

    @Override
    public void deleteByObjectId(String objectId) {
        try {
            commentsCollection.deleteOne(Filters.eq("_id", new ObjectId(objectId)));
        } catch (IllegalArgumentException e) {
            throw new DatabaseException("Invalid comment ID format: " + objectId, e);
        }
    }

    @Override
    public void deleteByPostId(int postId) {
        commentsCollection.deleteMany(Filters.eq("postId", postId));
    }

    /**
     * Map MongoDB Document to Comment object.
     * @param doc the MongoDB document
     * @return the Comment object
     */
    private Comment mapToComment(Document doc) {
        Comment comment = new Comment();
        comment.setId(doc.getObjectId("_id").toHexString());
        comment.setPostId(doc.getInteger("postId"));
        comment.setUserName(doc.getString("userName"));
        comment.setBody(doc.getString("body"));
        comment.setParentCommentId(doc.getInteger("parentCommentId"));

        return comment;
    }
}
