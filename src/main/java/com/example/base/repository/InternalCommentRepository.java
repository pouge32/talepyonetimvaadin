package com.example.base.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.base.entity.InternalCommentEntity;

public interface InternalCommentRepository extends JpaRepository<InternalCommentEntity, Integer> {

    @Query("SELECT c FROM InternalCommentEntity c " +
           "JOIN FETCH c.sender " +
           "LEFT JOIN FETCH c.receiver " +
           "WHERE c.request.requestId = :requestId " +
           "AND (c.receiver IS NULL OR c.sender.userId = :userId OR c.receiver.userId = :userId) " +
           "ORDER BY c.createdAt ASC")
    List<InternalCommentEntity> findVisibleCommentsForUser(@Param("requestId") Integer requestId, @Param("userId") Integer userId);

    @Query("SELECT COUNT(c) FROM InternalCommentEntity c " +
           "WHERE (c.receiver.userId = :userId OR (c.receiver IS NULL AND c.sender.userId != :userId)) " +
           "AND c.isRead = false")
    int countUnreadMessagesForUser(@Param("userId") Integer userId);
}