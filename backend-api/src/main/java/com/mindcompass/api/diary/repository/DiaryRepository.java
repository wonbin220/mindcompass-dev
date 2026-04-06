// 파일: DiaryRepository.java
// 역할: 일기 기본 CRUD Repository
// 설명: Spring Data JPA가 구현을 자동 생성한다

package com.mindcompass.api.diary.repository;

import com.mindcompass.api.diary.domain.Diary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DiaryRepository extends JpaRepository<Diary, Long> {

    // 사용자의 일기 목록 (페이징)
    Page<Diary> findByUserIdOrderByDiaryDateDesc(Long userId, Pageable pageable);

    // 사용자의 특정 날짜 일기
    Optional<Diary> findByUserIdAndDiaryDate(Long userId, LocalDate diaryDate);

    // 사용자의 기간별 일기 목록
    List<Diary> findByUserIdAndDiaryDateBetweenOrderByDiaryDateDesc(
            Long userId, LocalDate startDate, LocalDate endDate);

    // 일기 존재 확인 (특정 날짜)
    boolean existsByUserIdAndDiaryDate(Long userId, LocalDate diaryDate);
}
