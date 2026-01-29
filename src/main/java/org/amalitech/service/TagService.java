package org.amalitech.service;

import org.amalitech.dao.TagDao;
import org.amalitech.util.exception.ValidationException;
import org.amalitech.models.Tag;

import java.util.List;

public class TagService {

    private final TagDao tagDao;

    public TagService(TagDao tagDao) {
        this.tagDao = tagDao;
    }

    public void createTag(Tag tag) {
        validateTag(tag);
        tagDao.save(tag);
    }

    public Tag getTagById(int id) {
        if (id <= 0) throw new ValidationException("Invalid tag ID");
        return tagDao.findById(id);
    }

    public Tag getTagByName(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        return tagDao.findByName(name.trim());
    }

    public List<Tag> getAllTags() {
        return tagDao.findAll();
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
