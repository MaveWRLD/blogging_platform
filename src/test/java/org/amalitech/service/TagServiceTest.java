package org.amalitech.service;

import org.amalitech.interfaces.TagRepository;
import org.amalitech.models.Tag;
import org.amalitech.util.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class TagServiceTest {
    @Mock
    private TagRepository tagRepository;
    @InjectMocks
    private TagService tagService;
    @Test
    void createTag_createsValidTag() {
        Tag tag = new Tag();
        tag.setName("test");
        when(tagRepository.save(any(Tag.class))).thenReturn(1);
        tagService.createTag(tag);
        verify(tagRepository).save(tag);
    }
    @Test
    void createTag_throwsValidationForInvalidName() {
        Tag tag = new Tag();
        tag.setName("");
        assertThatThrownBy(() -> tagService.createTag(tag))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Tag name cannot be empty");
    }
    @Test
    void findTagsByPostId_returnsTags() {
        List<Tag> tags = List.of(new Tag());
        when(tagRepository.findTagsByPostId(1)).thenReturn(tags);
        List<Tag> result = tagService.findTagsByPostId(1);
        assertThat(result).isEqualTo(tags);
    }
    @Test
    void getTagById_returnsTag() {
        Tag tag = new Tag();
        when(tagRepository.findById(1)).thenReturn(List.of(tag));
        Tag result = tagService.getTagById(1);
        assertThat(result).isEqualTo(tag);
    }
    @Test
    void getTagByName_returnsTag() {
        Tag tag = new Tag();
        when(tagRepository.findByName("test")).thenReturn(List.of(tag));
        Tag result = tagService.getTagByName("test");
        assertThat(result).isEqualTo(tag);
    }
}
