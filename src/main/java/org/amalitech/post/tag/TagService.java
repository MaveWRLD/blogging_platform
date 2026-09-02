package org.amalitech.post.tag;

import org.amalitech.post.tag.TagRepository;
import org.amalitech.exception.ValidationException;
import org.amalitech.post.tag.Tag;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;


@Service
public class TagService {

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Transactional
    public Set<Tag> findOrCreateTagsByName(Set<String> names) {

        if (names == null || names.isEmpty()) {
            return Collections.emptySet();
        }

        Set<Tag> existingTags = tagRepository.findByNameIn(names);

        Set<String> existingNames = existingTags.stream()
                .map(Tag::getName)
                .collect(Collectors.toSet());

        Set<Tag> newTags = names.stream()
                .filter(name -> !existingNames.contains(name))
                .distinct()
                .map(name -> {
                    Tag tag = new Tag();
                    tag.setName(name);
                    return tag;
                }).collect(Collectors.toSet());

        if (!newTags.isEmpty()) {
            existingTags.addAll(tagRepository.saveAll(newTags));
        }

        return new HashSet<>(existingTags);
    }


    public Tag getTagById(int id) {
        if (id <= 0) throw new ValidationException("Invalid tag ID");
        var tags = tagRepository.findById(id);
        return tags.orElse(null);
    }
}
