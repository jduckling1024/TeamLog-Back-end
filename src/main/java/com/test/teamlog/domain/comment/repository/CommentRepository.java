package com.test.teamlog.domain.comment.repository;

import com.test.teamlog.domain.account.model.User;

import com.test.teamlog.domain.comment.entity.Comment;
import com.test.teamlog.domain.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostAndParentCommentIsNull(Post post);

    List<Comment> findAllByWriter(User user);

    @Query("SELECT c FROM Comment c WHERE c.post in (:posts)")
    List<Comment> findAllByPosts(@Param("posts") List<Post> post);

    @Query("SELECT c FROM Comment c WHERE c.post = :post AND c.parentComment is null")
    Page<Comment> findParentCommentListByPost(@Param("post") Post post, Pageable pageable);

    @Query("SELECT c FROM Comment c WHERE c.parentComment = :comment")
    Page<Comment> getChildCommentsByParentComment(@Param("comment") Comment comment, Pageable pageable);
}
