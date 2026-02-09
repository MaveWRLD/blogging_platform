package org.amalitech.service;

import org.amalitech.interfaces.TagRepository;
import org.amalitech.util.exception.ValidationException;
import org.amalitech.models.Tag;
import org.springframework.stereotype.Service;


import java.util.List;

@Service
public class TagService {

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    public void createTag(Tag tag) {
        validateTag(tag);
        tagRepository.save(tag);
    }

    public List<Tag> findTagsByPostId(int postId) {
        if (postId <= 0) throw new ValidationException("Invalid post ID");
        return tagRepository.findTagsByPostId(postId);
    }

    public Tag getTagById(int id) {
        if (id <= 0) throw new ValidationException("Invalid tag ID");
        var tags = tagRepository.findById(id);
        return tags.isEmpty() ? null : tags.get(0);
    }

    public Tag getTagByName(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        var tags = tagRepository.findByName(name.trim());
        return tags.isEmpty() ? null : tags.get(0);
    }


    private void validateTag(Tag tag) {
        if (tag.getName() == null || tag.getName().trim().isEmpty()) {
            throw new ValidationException("Tag name cannot be empty");
        }
        if (tag.getName().length() > 50) {
            throw new ValidationException("Tag name cannot exceed 50 characters");
        }
    }
}
