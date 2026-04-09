package com.mindcompass.api.diary.service;

import com.mindcompass.api.common.exception.BusinessException;
import com.mindcompass.api.common.exception.ErrorCode;
import com.mindcompass.api.diary.domain.Diary;
import com.mindcompass.api.diary.dto.request.CreateDiaryRequest;
import com.mindcompass.api.diary.dto.request.UpdateDiaryRequest;
import com.mindcompass.api.diary.dto.response.DiaryResponse;
import com.mindcompass.api.diary.repository.DiaryRepository;
import com.mindcompass.api.infra.ai.AiDiaryAnalysisClient;
import com.mindcompass.api.infra.ai.dto.DiaryAnalysisRequest;
import com.mindcompass.api.infra.ai.dto.DiaryAnalysisResponse;
import com.mindcompass.api.user.domain.User;
import com.mindcompass.api.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DiaryServiceTest {

    @InjectMocks
    private DiaryService diaryService;

    @Mock
    private DiaryRepository diaryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AiDiaryAnalysisClient aiDiaryAnalysisClient;

    @Test
    @DisplayName("일기 생성 성공 - AI 분석 성공")
    void createDiary_success_withAiAnalysis() {
        // given
        Long userId = 1L;
        User user = createUser();
        CreateDiaryRequest request = createCreateDiaryRequest();
        Diary savedDiary = createDiary(user);
        DiaryAnalysisResponse analysisResponse = createAnalysisResponse();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(diaryRepository.save(any(Diary.class))).willReturn(savedDiary);
        given(aiDiaryAnalysisClient.analyze(any(DiaryAnalysisRequest.class)))
                .willReturn(Optional.of(analysisResponse));

        // when
        DiaryResponse response = diaryService.createDiary(userId, request);

        // then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("오늘의 일기");
        assertThat(response.getContent()).isEqualTo("오늘은 좋은 하루였다");
        verify(diaryRepository).save(any(Diary.class));
        verify(aiDiaryAnalysisClient).analyze(any(DiaryAnalysisRequest.class));
    }

    @Test
    @DisplayName("일기 생성 성공 - AI 분석 실패해도 일기 저장됨")
    void createDiary_success_aiFailureDoesNotBlockSave() {
        // given
        Long userId = 1L;
        User user = createUser();
        CreateDiaryRequest request = createCreateDiaryRequest();
        Diary savedDiary = createDiary(user);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(diaryRepository.save(any(Diary.class))).willReturn(savedDiary);
        given(aiDiaryAnalysisClient.analyze(any(DiaryAnalysisRequest.class)))
                .willThrow(new RuntimeException("AI 서버 연결 실패"));

        // when
        DiaryResponse response = diaryService.createDiary(userId, request);

        // then - AI 실패해도 일기는 정상 반환
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("오늘의 일기");
        verify(diaryRepository).save(any(Diary.class));
    }

    @Test
    @DisplayName("일기 조회 성공")
    void getDiary_success() {
        // given
        Long userId = 1L;
        Long diaryId = 1L;
        User user = createUser();
        Diary diary = createDiary(user);

        given(diaryRepository.findById(diaryId)).willReturn(Optional.of(diary));

        // when
        DiaryResponse response = diaryService.getDiary(userId, diaryId);

        // then
        assertThat(response.getId()).isEqualTo(diaryId);
        assertThat(response.getTitle()).isEqualTo("오늘의 일기");
    }

    @Test
    @DisplayName("일기 조회 실패 - 일기 없음")
    void getDiary_fail_diaryNotFound() {
        // given
        Long userId = 1L;
        Long diaryId = 999L;

        given(diaryRepository.findById(diaryId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> diaryService.getDiary(userId, diaryId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DIARY_NOT_FOUND);
    }

    @Test
    @DisplayName("일기 조회 실패 - 다른 사용자의 일기")
    void getDiary_fail_accessDenied() {
        // given
        Long ownerId = 1L;
        Long otherUserId = 2L;
        Long diaryId = 1L;
        User owner = createUser();
        Diary diary = createDiary(owner);

        given(diaryRepository.findById(diaryId)).willReturn(Optional.of(diary));

        // when & then
        assertThatThrownBy(() -> diaryService.getDiary(otherUserId, diaryId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DIARY_ACCESS_DENIED);
    }

    @Test
    @DisplayName("일기 수정 성공 - 내용 수정 시 재분석")
    void updateDiary_success_withReanalysis() {
        // given
        Long userId = 1L;
        Long diaryId = 1L;
        User user = createUser();
        Diary diary = createDiary(user);
        UpdateDiaryRequest request = createUpdateDiaryRequest();
        DiaryAnalysisResponse analysisResponse = createAnalysisResponse();

        given(diaryRepository.findById(diaryId)).willReturn(Optional.of(diary));
        given(aiDiaryAnalysisClient.analyze(any(DiaryAnalysisRequest.class)))
                .willReturn(Optional.of(analysisResponse));

        // when
        DiaryResponse response = diaryService.updateDiary(userId, diaryId, request);

        // then
        assertThat(response.getId()).isEqualTo(diaryId);
        verify(aiDiaryAnalysisClient).analyze(any(DiaryAnalysisRequest.class));
    }

    @Test
    @DisplayName("일기 삭제 성공")
    void deleteDiary_success() {
        // given
        Long userId = 1L;
        Long diaryId = 1L;
        User user = createUser();
        Diary diary = createDiary(user);

        given(diaryRepository.findById(diaryId)).willReturn(Optional.of(diary));

        // when
        diaryService.deleteDiary(userId, diaryId);

        // then
        verify(diaryRepository).delete(diary);
    }

    @Test
    @DisplayName("일기 삭제 실패 - 일기 없음")
    void deleteDiary_fail_diaryNotFound() {
        // given
        Long userId = 1L;
        Long diaryId = 999L;

        given(diaryRepository.findById(diaryId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> diaryService.deleteDiary(userId, diaryId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DIARY_NOT_FOUND);
    }

    @Test
    @DisplayName("일기 재분석 성공")
    void analyzeDiary_success() {
        // given
        Long userId = 1L;
        Long diaryId = 1L;
        User user = createUser();
        Diary diary = createDiary(user);
        DiaryAnalysisResponse analysisResponse = createAnalysisResponse();

        given(diaryRepository.findById(diaryId)).willReturn(Optional.of(diary));
        given(aiDiaryAnalysisClient.analyze(any(DiaryAnalysisRequest.class)))
                .willReturn(Optional.of(analysisResponse));

        // when
        DiaryResponse response = diaryService.analyzeDiary(userId, diaryId);

        // then
        assertThat(response.getId()).isEqualTo(diaryId);
        verify(aiDiaryAnalysisClient).analyze(any(DiaryAnalysisRequest.class));
    }

    // --- 헬퍼 메서드 ---

    private User createUser() {
        User user = User.builder()
                .email("test@test.com")
                .password("encodedPassword")
                .name("testName")
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }

    private Diary createDiary(User user) {
        Diary diary = Diary.builder()
                .user(user)
                .title("오늘의 일기")
                .content("오늘은 좋은 하루였다")
                .diaryDate(LocalDate.of(2026, 4, 8))
                .build();
        ReflectionTestUtils.setField(diary, "id", 1L);
        return diary;
    }

    private CreateDiaryRequest createCreateDiaryRequest() {
        CreateDiaryRequest request = new CreateDiaryRequest();
        ReflectionTestUtils.setField(request, "title", "오늘의 일기");
        ReflectionTestUtils.setField(request, "content", "오늘은 좋은 하루였다");
        ReflectionTestUtils.setField(request, "diaryDate", LocalDate.of(2026, 4, 8));
        return request;
    }

    private UpdateDiaryRequest createUpdateDiaryRequest() {
        UpdateDiaryRequest request = new UpdateDiaryRequest();
        ReflectionTestUtils.setField(request, "title", "수정된 제목");
        ReflectionTestUtils.setField(request, "content", "수정된 내용");
        ReflectionTestUtils.setField(request, "diaryDate", LocalDate.of(2026, 4, 8));
        return request;
    }

    private DiaryAnalysisResponse createAnalysisResponse() {
        DiaryAnalysisResponse response = new DiaryAnalysisResponse();
        ReflectionTestUtils.setField(response, "primaryEmotion", "기쁨");
        ReflectionTestUtils.setField(response, "emotionScore", 0.85);
        ReflectionTestUtils.setField(response, "summary", "좋은 하루를 보냈다");
        ReflectionTestUtils.setField(response, "riskScore", 5);
        return response;
    }
}
