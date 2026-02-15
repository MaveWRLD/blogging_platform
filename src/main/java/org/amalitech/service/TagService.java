package org.amalitech.service;

import org.amalitech.repositories.TagRepository;
import org.amalitech.exception.ValidationException;
import org.amalitech.entities.Tag;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


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

    @Transactional
    public List<Tag> findOrCreateTags(List<String> names) {

        List<Tag> existing = tagRepository.findByNameIn(names);

        Set<String> existingNames = existing.stream()
                .map(Tag::getName)
                .collect(Collectors.toSet());

        List<Tag> toCreate = names.stream()
                .filter(name -> !existingNames.contains(name))
                .map(Tag::new)
                .toList();

        tagRepository.saveAll(toCreate);


        existing.addAll(toCreate);
        return existing;
    }


    public Tag getTagById(int id) {
        if (id <= 0) throw new ValidationException("Invalid tag ID");
        var tags = tagRepository.findById(id);
        return tags.orElse(null);
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
