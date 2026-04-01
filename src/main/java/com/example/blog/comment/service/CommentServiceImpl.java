package com.example.blog.comment.service;

import com.example.blog.comment.entity.Comment;
import com.example.blog.comment.presentation.dto.request.CommentCreateRequest;
import com.example.blog.comment.presentation.dto.request.CommentUpdateRequest;
import com.example.blog.comment.presentation.dto.response.CommentDeleteResponse;
import com.example.blog.comment.presentation.dto.response.CommentResponse;
import com.example.blog.comment.repository.CommentRepository;
import com.example.blog.global.exception.BlogException;
import com.example.blog.global.exception.ErrorCode;
import com.example.blog.post.entity.Post;
import com.example.blog.post.repository.PostRepository;
import com.example.blog.user.entity.User;
import com.example.blog.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public CommentResponse createComment(Long postId, Long loginUserId, CommentCreateRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BlogException(ErrorCode.POST_NOT_FOUND));

        User user = userRepository.findById(loginUserId)
                .orElseThrow(() -> new BlogException(ErrorCode.USER_NOT_FOUND));

        Comment comment = Comment.create(post, user, request.content());
        Comment saved = commentRepository.save(comment);

        return CommentResponse.from(saved, loginUserId);
    }

    @Override
    public List<CommentResponse> getComments(Long postId, Long loginUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BlogException(ErrorCode.POST_NOT_FOUND));

        return commentRepository.findAllByPostOrderByCreatedAtAsc(post)
                .stream()
                .map(comment -> CommentResponse.from(comment, loginUserId))
                .toList();
    }

    @Override
    @Transactional
    public CommentResponse updateComment(Long commentId, Long loginUserId, CommentUpdateRequest request) {
        Comment comment = commentRepository.findWithAuthorAndPostById(commentId)
                .orElseThrow(() -> new BlogException(ErrorCode.COMMENT_NOT_FOUND));

        validateOwner(comment, loginUserId);
        comment.updateContent(request.content());

        return CommentResponse.from(comment, loginUserId);
    }

    @Override
    @Transactional
    public CommentDeleteResponse deleteComment(Long commentId, Long loginUserId) {
        Comment comment = commentRepository.findWithAuthorAndPostById(commentId)
                .orElseThrow(() -> new BlogException(ErrorCode.COMMENT_NOT_FOUND));

        validateOwner(comment, loginUserId);
        comment.softDelete();

        return CommentDeleteResponse.of(comment.getId());
    }

    private void validateOwner(Comment comment, Long loginUserId) {
        if (!comment.getAuthor().getUserId().equals(loginUserId)) {
            throw new BlogException(ErrorCode.COMMENT_FORBIDDEN);
        }
    }
}