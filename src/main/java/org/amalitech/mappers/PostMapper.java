package org.amalitech.mappers;

import org.amalitech.dtos.*;
import org.amalitech.dtos.postDtos.*;
import org.amalitech.models.Post;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PostMapper {

    PostDto toDto(Post post);

    PostWithCommentsDto toDtoWithComments(PostDto postDto, List<CommentDto> comments);

    PagedPostsResponse toPagedResponse(
            List<Post> post, int page, int size, long total, int totalPages, boolean hasPrevious, boolean hasNext
    );

    Post toEntity(CreatePostRequest createPostRequest);

    void updateEntity(UpdatePostRequest updatePostRequest, @MappingTarget Post post);
}