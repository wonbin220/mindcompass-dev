// 파일: ChatMessageRepository.java
// 역할: 채팅 메시지 Repository

package com.mindcompass.api.chat.repository;

import com.mindcompass.api.chat.domain.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 세션의 메시지 목록
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);

    // 세션의 최근 메시지 (페이징)
    Page<ChatMessage> findBySessionIdOrderByCreatedAtDesc(Long sessionId, Pageable pageable);

    // 세션의 메시지 수
    long countBySessionId(Long sessionId);
}
