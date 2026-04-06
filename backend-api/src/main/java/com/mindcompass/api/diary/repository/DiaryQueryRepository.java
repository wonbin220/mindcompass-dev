// 파일: DiaryQueryRepository.java
// 역할: 일기 복잡 조회용 QueryRepository
// 설명: 복잡한 조회 쿼리는 여기서 처리한다 (QueryDSL 또는 JPQL)
// 예시: 감정별 필터링, 통계 조회 등

package com.mindcompass.api.diary.repository;

import com.mindcompass.api.diary.domain.Diary;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class DiaryQueryRepository {

    private final EntityManager em;

    /**
     * 감정별 일기 목록 조회
     */
    public List<Diary> findByUserIdAndEmotion(Long userId, String emotion, int limit) {
        String jpql = """
            SELECT d FROM Diary d
            WHERE d.user.id = :userId
              AND d.primaryEmotion = :emotion
            ORDER BY d.diaryDate DESC
            """;

        return em.createQuery(jpql, Diary.class)
                .setParameter("userId", userId)
                .setParameter("emotion", emotion)
                .setMaxResults(limit)
                .getResultList();
    }

    /**
     * 기간 내 감정 분포 통계
     * TODO: 실제 구현 시 집계 쿼리 작성
     */
    public Map<String, Long> getEmotionDistribution(Long userId, LocalDate startDate, LocalDate endDate) {
        // TODO: 감정별 카운트 집계 쿼리 구현
        // SELECT d.primaryEmotion, COUNT(d) FROM Diary d ...
        return Map.of();
    }

    /**
     * 미분석 일기 목록 조회 (배치 처리용)
     */
    public List<Diary> findUnanalyzedDiaries(int limit) {
        String jpql = """
            SELECT d FROM Diary d
            WHERE d.isAnalyzed = false
            ORDER BY d.createdAt ASC
            """;

        return em.createQuery(jpql, Diary.class)
                .setMaxResults(limit)
                .getResultList();
    }
}
