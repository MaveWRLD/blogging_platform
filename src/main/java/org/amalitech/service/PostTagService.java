package org.amalitech.service;

import org.amalitech.interfaces.PostTagRepository;
import org.amalitech.util.exception.ValidationException;
import org.amalitech.models.PostTag;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PostTagService {

    private final PostTagRepository postTagRepository;

    public PostTagService(PostTagRepository postTagRepository) {
        this.postTagRepository = postTagRepository;
    }

    public void addTagToPost(int postId, int tagId) {
        validateIds(postId, tagId);
        PostTag postTag = new PostTag(postId, tagId);
        postTagRepository.save(postTag);
    }

    public void addTagsToPost(int postId, List<Integer> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) return;
        for (Integer tagId : tagIds) {
            addTagToPost(postId, tagId);
        }
    }

    public void removeAllTagsFromPost(int postId) {
        if (postId <= 0) {
            throw new ValidationException("Invalid post ID");
        }
        postTagRepository.deleteAllTagsForPost(postId);
    }

    private void validateIds(int postId, int tagId) {
        if (postId <= 0) throw new ValidationException("Invalid post ID");
        if (tagId <= 0) throw new ValidationException("Invalid tag ID");
    }
}
