// 파일: UserRepository.java
// 역할: 사용자 기본 CRUD Repository
// 설명: Spring Data JPA가 구현을 자동 생성한다

package com.mindcompass.api.user.repository;

import com.mindcompass.api.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 이메일로 사용자 조회
    Optional<User> findByEmail(String email);

    // 이메일 존재 여부 확인
    boolean existsByEmail(String email);
}
