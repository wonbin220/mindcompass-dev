// 파일: ChatSessionRepository.java
// 역할: 채팅 세션 Repository

package com.mindcompass.api.chat.repository;

import com.mindcompass.api.chat.domain.ChatSession;
import com.mindcompass.api.chat.domain.SessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    // 사용자의 세션 목록 (최신순)
    Page<ChatSession> findByUserIdOrderByUpdatedAtDesc(Long userId, Pageable pageable);

    // 사용자의 활성 세션 목록
    List<ChatSession> findByUserIdAndStatusOrderByUpdatedAtDesc(Long userId, SessionStatus status);

    // 기간 내 채팅 세션 수 조회
    long countByUserIdAndCreatedAtBetween(Long userId, java.time.LocalDateTime start, java.time.LocalDateTime end);

    // 세션과 메시지 함께 조회 (N+1 방지)
    // @Query("SELECT s FROM ChatSession s LEFT JOIN FETCH s.messages WHERE s.id = :id")
    // Optional<ChatSession> findByIdWithMessages(@Param("id") Long id);
}
