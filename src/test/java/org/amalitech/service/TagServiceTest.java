package org.amalitech.service;

import org.amalitech.entities.Tag;
import org.amalitech.exception.ValidationException;
import org.amalitech.repositories.TagRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TagService Tests")
class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private TagService tagService;

    @Nested
    @DisplayName("findOrCreateTagsByName()")
    class FindOrCreateTagsByName {

        @Test
        @DisplayName("returns existing tags without creating new ones")
        void allTagsExist_returnsExistingSet() {
            Tag java = tagWithName(1, "java");
            Tag spring = tagWithName(2, "spring");

            when(tagRepository.findByNameIn(Set.of("java", "spring")))
                    .thenReturn(Set.of(java, spring));

            Set<Tag> result = tagService.findOrCreateTagsByName(Set.of("java", "spring"));

            assertThat(result).containsExactlyInAnyOrder(java, spring);
            verify(tagRepository, never()).saveAll(anyCollection());
        }

        @Test
        @DisplayName("returns empty set when input is empty")
        void emptyInput_returnsEmptySet() {
            when(tagRepository.findByNameIn(Set.of())).thenReturn(Set.of());

            Set<Tag> result = tagService.findOrCreateTagsByName(Set.of());

            assertThat(result).isEmpty();
            verify(tagRepository, never()).saveAll(anyCollection());
        }
    }

    @Nested
    @DisplayName("getTagById()")
    class GetTagById {

        @Test
        @DisplayName("returns tag when found")
        void validId_returnsTag() {
            Tag tag = tagWithName(1, "java");
            when(tagRepository.findById(1)).thenReturn(Optional.of(tag));

            Tag result = tagService.getTagById(1);

            assertThat(result).isEqualTo(tag);
        }

        @Test
        @DisplayName("returns null when tag does not exist")
        void idNotFound_returnsNull() {
            when(tagRepository.findById(99)).thenReturn(Optional.empty());

            Tag result = tagService.getTagById(99);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("throws ValidationException when id is zero")
        void idIsZero_throwsValidationException() {
            assertThatThrownBy(() -> tagService.getTagById(0))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Invalid tag ID");
            verifyNoInteractions(tagRepository);
        }

        @Test
        @DisplayName("throws ValidationException when id is negative")
        void negativeId_throwsValidationException() {
            assertThatThrownBy(() -> tagService.getTagById(-1))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Invalid tag ID");
        }
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------
    private Tag tagWithName(int id, String name) {
        Tag tag = new Tag();
        tag.setId(id);
        tag.setName(name);
        return tag;
    }
}