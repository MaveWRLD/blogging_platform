package org.amalitech.service;

import org.amalitech.dao.PostTagDao;
import org.amalitech.util.exception.ValidationException;
import org.amalitech.models.PostTag;

import java.util.List;

public class PostTagService {

    private final PostTagDao postTagDao;

    public PostTagService(PostTagDao postTagDao) {
        this.postTagDao = postTagDao;
    }

    public void addTagToPost(int postId, int tagId) {
        validateIds(postId, tagId);
        PostTag postTag = new PostTag(postId, tagId);
        postTagDao.save(postTag);
    }

    public void addTagsToPost(int postId, List<Integer> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) return;
        for (Integer tagId : tagIds) {
            addTagToPost(postId, tagId);
        }
    }

    public void removeTagFromPost(int postId, int tagId) {
        validateIds(postId, tagId);
        PostTag postTag = new PostTag(postId, tagId);
        postTagDao.delete(postTag);
    }

    public void removeAllTagsFromPost(int postId) {
        if (postId <= 0) {
            throw new ValidationException("Invalid post ID");
        }
        postTagDao.deleteAllTagsForPost(postId);
    }

    public List<Integer> getTagsForPost(int postId) {
        if (postId <= 0) {
            throw new ValidationException("Invalid post ID");
        }
        return postTagDao.findTagsByPostId(postId);
    }

    private void validateIds(int postId, int tagId) {
        if (postId <= 0) throw new ValidationException("Invalid post ID");
        if (tagId <= 0) throw new ValidationException("Invalid tag ID");
    }
}
