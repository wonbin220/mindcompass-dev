// 파일: RefreshTokenRepository.java
// 역할: RefreshToken 저장/조회/revoke
// 호출: AuthService -> RefreshTokenRepository

package com.mindcompass.api.auth.repository;

import com.mindcompass.api.auth.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    // DB에는 원본 JWT가 아닌 SHA-256 해시만 저장한다.
    // DB가 유출되더라도 해시만으로는 원본 토큰을 복원할 수 없어서 토큰 도용을 막는다.
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    // 로그아웃 시 해당 사용자의 유효한 토큰을 한 번에 전부 폐기한다.
    // @Modifying: SELECT가 아닌 UPDATE/DELETE 쿼리임을 JPA에 알리는 어노테이션이다. 없으면 예외가 발생한다.
    // revokedAt IS NULL 조건: 이미 폐기된 토큰은 다시 건드리지 않기 위해서다.
    @Modifying
    @Query("UPDATE RefreshToken r SET r.revokedAt = :now WHERE r.user.id = :userId AND r.revokedAt IS NULL")
    void revokeAllByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
