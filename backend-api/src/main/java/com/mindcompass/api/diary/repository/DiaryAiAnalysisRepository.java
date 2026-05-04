// 파일: DiaryAiAnalysisRepository.java
// 역할: 일기 AI 분석 저장소
// 호출: DiaryService -> DiaryAiAnalysisRepository

package com.mindcompass.api.diary.repository;

import com.mindcompass.api.diary.domain.DiaryAiAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiaryAiAnalysisRepository extends JpaRepository<DiaryAiAnalysis, Long> {
}
