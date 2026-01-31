package org.amalitech.mappers;

import org.amalitech.dtos.CreatePostRequest;
import org.amalitech.dtos.PostDto;
import org.amalitech.dtos.UpdatePostRequest;
import org.amalitech.models.Post;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PostMapper {

    PostDto toDto(Post post);

    Post toEntity(CreatePostRequest createPostRequest);

    void updateEntity(UpdatePostRequest updatePostRequest, @MappingTarget Post post);
}
