// 파일: DiaryService.java
// 역할: 일기 비즈니스 로직
// 흐름: DiaryController -> DiaryService -> DiaryRepository, AiDiaryAnalysisClient
// 핵심 원칙: AI 실패가 일기 저장을 막으면 안 된다

package com.mindcompass.api.diary.service;

import com.mindcompass.api.common.exception.BusinessException;
import com.mindcompass.api.common.exception.ErrorCode;
import com.mindcompass.api.diary.domain.Diary;
import com.mindcompass.api.diary.dto.request.CreateDiaryRequest;
import com.mindcompass.api.diary.dto.request.UpdateDiaryRequest;
import com.mindcompass.api.diary.dto.response.DiaryListResponse;
import com.mindcompass.api.diary.dto.response.DiaryResponse;
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

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryService {

    private final DiaryRepository diaryRepository;
    private final UserRepository userRepository;
    private final AiDiaryAnalysisClient aiDiaryAnalysisClient;

    /**
     * 일기 생성
     *
     * 실행 흐름:
     * 1. 사용자 조회
     * 2. 일기 엔티티 생성 및 저장 (먼저 저장!)
     * 3. AI 분석 요청 (비동기 또는 fallback)
     * 4. 분석 결과 저장 (성공 시에만)
     */
    @Transactional
    public DiaryResponse createDiary(Long userId, CreateDiaryRequest request) {
        User user = findUserById(userId);

        // 1. 일기 먼저 저장 (AI 실패와 무관하게)
        Diary diary = Diary.builder()
                .user(user)
                .title(request.getTitle())
                .content(request.getContent())
                .diaryDate(request.getDiaryDate())
                .build();

        diary = diaryRepository.save(diary);
        log.info("일기 저장 완료: diaryId={}, userId={}", diary.getId(), userId);

        // 2. AI 분석 시도 (실패해도 일기는 이미 저장됨)
        tryAnalyzeDiary(diary);

        return DiaryResponse.from(diary);
    }

    /**
     * 일기 단건 조회
     */
    public DiaryResponse getDiary(Long userId, Long diaryId) {
        Diary diary = findDiaryById(diaryId);
        validateOwnership(diary, userId);
        return DiaryResponse.from(diary);
    }

    /**
     * 일기 목록 조회
     */
    public Page<DiaryListResponse> getDiaries(Long userId, Pageable pageable) {
        return diaryRepository.findByUserIdOrderByDiaryDateDesc(userId, pageable)
                .map(DiaryListResponse::from);
    }

    /**
     * 일기 수정
     */
    @Transactional
    public DiaryResponse updateDiary(Long userId, Long diaryId, UpdateDiaryRequest request) {
        Diary diary = findDiaryById(diaryId);
        validateOwnership(diary, userId);

        diary.update(request.getTitle(), request.getContent(), request.getDiaryDate());
        log.info("일기 수정 완료: diaryId={}", diaryId);

        // 내용이 수정되면 재분석
        if (request.getContent() != null) {
            tryAnalyzeDiary(diary);
        }

        return DiaryResponse.from(diary);
    }

    /**
     * 일기 삭제
     */
    @Transactional
    public void deleteDiary(Long userId, Long diaryId) {
        Diary diary = findDiaryById(diaryId);
        validateOwnership(diary, userId);

        diaryRepository.delete(diary);
        log.info("일기 삭제 완료: diaryId={}", diaryId);
    }

    /**
     * 일기 재분석 요청
     */
    @Transactional
    public DiaryResponse analyzeDiary(Long userId, Long diaryId) {
        Diary diary = findDiaryById(diaryId);
        validateOwnership(diary, userId);

        tryAnalyzeDiary(diary);
        return DiaryResponse.from(diary);
    }

    /**
     * AI 분석 시도 (실패해도 예외 던지지 않음)
     */
    private void tryAnalyzeDiary(Diary diary) {
        try {
            DiaryAnalysisRequest request = DiaryAnalysisRequest.builder()
                    .diaryId(diary.getId())
                    .userId(diary.getUser().getId())
                    .title(diary.getTitle())
                    .content(diary.getContent())
                    .build();

            Optional<DiaryAnalysisResponse> response = aiDiaryAnalysisClient.analyze(request);

            response.ifPresent(r -> {
                diary.applyAnalysis(
                        r.getPrimaryEmotion(),
                        r.getEmotionScore(),
                        r.getSummary(),
                        r.getRiskScore()
                );
                log.info("AI 분석 결과 적용: diaryId={}, emotion={}",
                        diary.getId(), r.getPrimaryEmotion());
            });

        } catch (Exception e) {
            // AI 실패해도 로그만 남기고 진행
            log.warn("AI 분석 실패 (일기 저장에는 영향 없음): diaryId={}, error={}",
                    diary.getId(), e.getMessage());
        }
    }

    // 공통 조회 메서드
    private Diary findDiaryById(Long diaryId) {
        return diaryRepository.findById(diaryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DIARY_NOT_FOUND));
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
}
