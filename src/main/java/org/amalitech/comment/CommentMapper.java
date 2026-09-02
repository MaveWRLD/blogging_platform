package org.amalitech.comment;

import org.amalitech.comment.dto.CommentDto;
import org.amalitech.comment.dto.CreateCommentRequest;
import org.amalitech.comment.dto.UpdateCommentRequest;
import org.amalitech.comment.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    Comment toEntity(CreateCommentRequest request);

    CommentDto toDto(Comment comment);

    void updateEntity(UpdateCommentRequest request, @MappingTarget Comment comment);
}
