// 파일: ChatSession.java
// 역할: 채팅 세션 엔티티
// 테이블: chat_sessions
// 설명: 사용자와 AI 간의 대화 세션을 저장한다

package com.mindcompass.api.chat.domain;

import com.mindcompass.api.user.domain.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chat_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 200)
    private String title;  // 세션 제목 (첫 메시지 기반 자동 생성)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status = SessionStatus.ACTIVE;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<ChatMessage> messages = new ArrayList<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public ChatSession(User user, String title) {
        this.user = user;
        this.title = title;
        this.status = SessionStatus.ACTIVE;
    }

    // 세션 제목 업데이트 (첫 메시지 기반)
    public void updateTitleIfEmpty(String firstMessage) {
        if (this.title == null && firstMessage != null) {
            this.title = firstMessage.length() > 50
                    ? firstMessage.substring(0, 50) + "..."
                    : firstMessage;
        }
    }

    // 세션 종료
    public void close() {
        this.status = SessionStatus.CLOSED;
    }

    // 소유자 확인
    public boolean isOwnedBy(Long userId) {
        return this.user.getId().equals(userId);
    }
}
