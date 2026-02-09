package org.amalitech.mappers;

import org.amalitech.dtos.CommentDto;
import org.amalitech.dtos.CreateCommentRequest;
import org.amalitech.dtos.UpdateCommentRequest;
import org.amalitech.models.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    Comment toEntity(CreateCommentRequest request);

    CommentDto toDto(Comment comment);

    void updateEntity(UpdateCommentRequest request, @MappingTarget Comment comment);
}
