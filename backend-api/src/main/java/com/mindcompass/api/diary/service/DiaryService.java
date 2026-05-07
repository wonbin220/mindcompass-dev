// 파일: DiaryService.java
// 역할: 일기 비즈니스 로직
// 호출: DiaryController -> DiaryService -> DiaryRepository, AI 저장소

package com.mindcompass.api.diary.service;

import com.mindcompass.api.common.exception.BusinessException;
import com.mindcompass.api.common.exception.ErrorCode;
import com.mindcompass.api.diary.domain.Diary;
import com.mindcompass.api.diary.domain.DiaryAiAnalysis;
import com.mindcompass.api.diary.domain.DiaryEmotionTag;
import com.mindcompass.api.diary.dto.request.CreateDiaryRequest;
import com.mindcompass.api.diary.dto.request.UpdateDiaryRequest;
import com.mindcompass.api.diary.dto.response.DiaryListResponse;
import com.mindcompass.api.diary.dto.response.DiaryResponse;
import com.mindcompass.api.diary.repository.DiaryAiAnalysisRepository;
import com.mindcompass.api.diary.repository.DiaryEmotionTagRepository;
import com.mindcompass.api.diary.repository.DiaryRepository;
import com.mindcompass.api.infra.ai.AiDiaryAnalysisClient;
import com.mindcompass.api.infra.ai.dto.DiaryAnalysisRequest;
import com.mindcompass.api.infra.ai.dto.DiaryAnalysisResponse;
import com.mindcompass.api.user.domain.User;
import com.mindcompass.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryService {

    private final DiaryRepository diaryRepository;
    private final DiaryAiAnalysisRepository diaryAiAnalysisRepository;
    private final DiaryEmotionTagRepository diaryEmotionTagRepository;
    private final UserRepository userRepository;
    private final AiDiaryAnalysisClient aiDiaryAnalysisClient;

    @Transactional
    public DiaryResponse createDiary(Long userId, CreateDiaryRequest request) {
        User user = findUserById(userId);

        LocalDate writtenDate = request.getWrittenAt().toLocalDate();
        long dailyCount = diaryRepository.countByUserIdAndWrittenAtBetweenAndDeletedAtIsNull(
                userId,
                writtenDate.atStartOfDay(),
                writtenDate.plusDays(1).atStartOfDay());
        if (dailyCount >= 3) {
            throw new BusinessException(ErrorCode.DIARY_DAILY_LIMIT_EXCEEDED);
        }

        Diary diary = Diary.builder()
                .user(user)
                .title(request.getTitle())
                .content(request.getContent())
                .writtenAt(request.getWrittenAt())
                .primaryEmotion(request.getPrimaryEmotion())
                .emotionIntensity(request.getEmotionIntensity())
                .build();

        diary = diaryRepository.save(diary);
        syncUserEmotionTag(diary);
        log.info("일기 저장 완료: diaryId={}, userId={}", diary.getId(), userId);

        tryAnalyzeDiary(diary);
        return DiaryResponse.from(diary);
    }

    public DiaryResponse getDiary(Long userId, Long diaryId) {
        Diary diary = findDiaryById(diaryId);
        validateOwnership(diary, userId);
        return DiaryResponse.from(diary);
    }

    public Page<DiaryListResponse> getDiaries(Long userId, Pageable pageable) {
        return diaryRepository.findByUserIdAndDeletedAtIsNullOrderByWrittenAtDesc(userId, pageable)
                .map(DiaryListResponse::from);
    }

    @Transactional
    public DiaryResponse updateDiary(Long userId, Long diaryId, UpdateDiaryRequest request) {
        Diary diary = findDiaryById(diaryId);
        validateOwnership(diary, userId);

        diary.update(request.getTitle(), request.getContent(), request.getWrittenAt());
        log.info("일기 수정 완료: diaryId={}", diaryId);

        if (request.getContent() != null || request.getTitle() != null) {
            tryAnalyzeDiary(diary);
        }

        return DiaryResponse.from(diary);
    }

    @Transactional
    public void deleteDiary(Long userId, Long diaryId) {
        Diary diary = findDiaryById(diaryId);
        validateOwnership(diary, userId);

        diary.delete();
        log.info("일기 삭제 완료: diaryId={}", diaryId);
    }

    @Transactional
    public DiaryResponse analyzeDiary(Long userId, Long diaryId) {
        Diary diary = findDiaryById(diaryId);
        validateOwnership(diary, userId);

        tryAnalyzeDiary(diary);
        return DiaryResponse.from(diary);
    }

    private void tryAnalyzeDiary(Diary diary) {
        try {
            DiaryAnalysisRequest request = DiaryAnalysisRequest.builder()
                    .diaryId(diary.getId())
                    .userId(diary.getUser().getId())
                    .title(diary.getTitle())
                    .content(diary.getContent())
                    .build();

            Optional<DiaryAnalysisResponse> response = aiDiaryAnalysisClient.analyze(request);
            response.ifPresent(r -> applyAnalysis(diary, r));
        } catch (Exception e) {
            log.warn("AI 분석 실패 (일기 저장에는 영향 없음): diaryId={}, error={}",
                    diary.getId(), e.getMessage());
        }
    }

    private void applyAnalysis(Diary diary, DiaryAnalysisResponse response) {
        DiaryAiAnalysis aiAnalysis = DiaryAiAnalysis.builder()
                .diary(diary)
                .primaryEmotion(response.getPrimaryEmotion())
                .emotionIntensity(response.getEmotionIntensity())
                .summary(response.getSummary())
                .confidence(response.getConfidence())
                .rawPayload(response.getRawPayload())
                .riskLevel(response.getRiskLevel())
                .riskScore(toBigDecimal(response.getRiskScore()))
                .riskSignals(response.getRiskSignals())
                .recommendedAction(response.getRecommendedAction())
                .build();

        diary.addAiAnalysis(aiAnalysis);
        diaryAiAnalysisRepository.save(aiAnalysis);
        syncAiEmotionTag(diary, response);

        log.info("AI 분석 결과 적용: diaryId={}, emotion={}",
                diary.getId(), response.getPrimaryEmotion());
    }

    private void syncUserEmotionTag(Diary diary) {
        diary.removeEmotionTagsBySourceType(DiaryEmotionTag.SOURCE_USER);

        if (diary.getPrimaryEmotion() == null || diary.getPrimaryEmotion().isBlank()) {
            return;
        }

        DiaryEmotionTag tag = DiaryEmotionTag.builder()
                .diary(diary)
                .emotionCode(diary.getPrimaryEmotion())
                .intensity(diary.getEmotionIntensity())
                .sourceType(DiaryEmotionTag.SOURCE_USER)
                .build();

        diary.addEmotionTag(tag);
        diaryEmotionTagRepository.save(tag);
    }

    private void syncAiEmotionTag(Diary diary, DiaryAnalysisResponse response) {
        diary.removeEmotionTagsBySourceType(DiaryEmotionTag.SOURCE_AI_ANALYSIS);

        if (response.getPrimaryEmotion() == null || response.getPrimaryEmotion().isBlank()) {
            return;
        }

        DiaryEmotionTag tag = DiaryEmotionTag.builder()
                .diary(diary)
                .emotionCode(response.getPrimaryEmotion())
                .intensity(response.getEmotionIntensity())
                .sourceType(DiaryEmotionTag.SOURCE_AI_ANALYSIS)
                .build();

        diary.addEmotionTag(tag);
        diaryEmotionTagRepository.save(tag);
    }

    private Diary findDiaryById(Long diaryId) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DIARY_NOT_FOUND));

        if (diary.isDeleted()) {
            throw new BusinessException(ErrorCode.DIARY_NOT_FOUND);
        }
        return diary;
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private void validateOwnership(Diary diary, Long userId) {
        if (!diary.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.DIARY_ACCESS_DENIED);
        }
    }

    private BigDecimal toBigDecimal(Number value) {
        return value == null ? null : BigDecimal.valueOf(value.doubleValue());
    }
}
