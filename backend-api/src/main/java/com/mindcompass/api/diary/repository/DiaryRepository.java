// 파일: DiaryRepository.java
// 역할: 일기 기본 CRUD Repository
// 호출: DiaryService, CalendarService, ReportService -> DiaryRepository

package com.mindcompass.api.diary.repository;

import com.mindcompass.api.diary.domain.Diary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DiaryRepository extends JpaRepository<Diary, Long> {

    // 사용자의 일기 목록 (페이징)
    Page<Diary> findByUserIdAndDeletedAtIsNullOrderByWrittenAtDesc(Long userId, Pageable pageable);

    // 사용자의 특정 날짜 일기
    Optional<Diary> findFirstByUserIdAndWrittenAtBetweenAndDeletedAtIsNullOrderByWrittenAtAsc(
            Long userId, LocalDateTime startDateTime, LocalDateTime endDateTime);

    // 사용자의 기간별 일기 목록
    List<Diary> findByUserIdAndWrittenAtBetweenAndDeletedAtIsNullOrderByWrittenAtDesc(
            Long userId, LocalDateTime startDateTime, LocalDateTime endDateTime);

    // 일기 존재 확인 (특정 날짜)
    boolean existsByUserIdAndWrittenAtBetweenAndDeletedAtIsNull(
            Long userId, LocalDateTime startDateTime, LocalDateTime endDateTime);

    // 특정 날짜 일기 수 (일일 제한 체크용)
    long countByUserIdAndWrittenAtBetweenAndDeletedAtIsNull(
            Long userId, LocalDateTime startDateTime, LocalDateTime endDateTime);

    // keyword는 호출 전에 DiaryService.buildLikePattern()으로 이스케이프 + % 래핑된 값을 받는다.
    // ESCAPE '!': %, _, ! 자체를 리터럴로 취급하게 한다. %keyword% 패턴 대신 :keyword로 받는 이유는
    // 이스케이프된 값에 % 래핑까지 서비스에서 처리해서 넘기기 위해서다.
    @Query("SELECT d FROM Diary d WHERE d.user.id = :userId " +
            "AND d.deletedAt IS NULL " +
            "AND (d.title LIKE :keyword ESCAPE '!' OR d.content LIKE :keyword ESCAPE '!') " +
            "ORDER BY d.writtenAt DESC")
    Page<Diary> searchByKeyword(
            @Param("userId") Long userId,
            @Param("keyword") String keyword,
            Pageable pageable);
}
