package org.amalitech.mappers;

import org.amalitech.dtos.*;
import org.amalitech.dtos.postDtos.*;
import org.amalitech.entities.Post;
import org.amalitech.factories.PostFactory;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PostMapper {


    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.username", target = "author")
    PostDto toDto(Post post);

    PostWithCommentsDto toDtoWithComments(PostDto postDto, List<CommentDto> comments);

    @ObjectFactory
    @Mapping(target = "tags", ignore = true)
    default Post createPost(CreatePostRequest request) {
        return PostFactory.fromRequest(request);
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "likeCount", ignore = true)
    @Mapping(target = "viewCount", ignore = true)
    @Mapping(target = "commentCount", ignore = true)
    void updateEntity(UpdatePostRequest updatePostRequest, @MappingTarget Post post);
}