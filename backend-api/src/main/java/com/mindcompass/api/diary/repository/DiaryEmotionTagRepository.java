// 파일: DiaryEmotionTagRepository.java
// 역할: 일기 감정 태그 저장소
// 호출: DiaryService -> DiaryEmotionTagRepository

package com.mindcompass.api.diary.repository;

import com.mindcompass.api.diary.domain.DiaryEmotionTag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiaryEmotionTagRepository extends JpaRepository<DiaryEmotionTag, Long> {
}
