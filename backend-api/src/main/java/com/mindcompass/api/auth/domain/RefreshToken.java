// 파일: RefreshToken.java
// 역할: 리프레시 토큰 엔티티
// 호출: AuthService -> RefreshToken

package com.mindcompass.api.auth.domain;

import com.mindcompass.api.user.domain.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, length = 255)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public RefreshToken(User user, String tokenHash, LocalDateTime expiresAt) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    // 토큰을 무효화한다. DB에서 삭제하지 않고 revokedAt 시각을 기록하는 방식(soft delete)을 쓰는 이유:
    // 나중에 "이 토큰은 언제 폐기됐는가"를 감사 로그 수준에서 추적할 수 있기 때문이다.
    public void revoke() {
        this.revokedAt = LocalDateTime.now();
    }

    // revokedAt이 null이면 아직 유효한 토큰, 값이 있으면 이미 폐기된 토큰이다.
    public boolean isRevoked() {
        return this.revokedAt != null;
    }
}
