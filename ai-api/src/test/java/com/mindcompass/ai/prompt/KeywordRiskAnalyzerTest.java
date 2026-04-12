// 파일: KeywordRiskAnalyzerTest.java
// 역할: 키워드 기반 위험도 분석기의 순수 로직을 검증한다.

package com.mindcompass.ai.prompt;

import com.mindcompass.ai.dto.response.RiskScoreResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KeywordRiskAnalyzerTest {

    private final KeywordRiskAnalyzer keywordRiskAnalyzer = new KeywordRiskAnalyzer();

    @Test
    @DisplayName("고위험 자살 관련 키워드가 포함되면 즉시 위험으로 판단한다")
    void analyze_returnsSuicideRisk_whenHighRiskSuicideKeywordExists() {
        // when
        RiskScoreResponse response = keywordRiskAnalyzer.analyze("요즘 자살 생각이 자꾸 들고 정말 죽고 싶어요.");

        // then
        assertThat(response.getIsRisky()).isTrue();
        assertThat(response.getRiskScore()).isEqualTo(90);
        assertThat(response.getRiskType()).isEqualTo("SUICIDE");
        assertThat(response.getAnalysisMethod()).isEqualTo("KEYWORD");
        assertThat(response.getRecommendedAction()).isEqualTo("SHOW_SAFETY_MESSAGE");
        assertThat(response.getDetectedKeywords()).isNotEmpty();
    }

    @Test
    @DisplayName("고위험 자해 관련 키워드가 포함되면 SELF_HARM으로 판단한다")
    void analyze_returnsSelfHarmRisk_whenHighRiskSelfHarmKeywordExists() {
        // when
        RiskScoreResponse response = keywordRiskAnalyzer.analyze("손목을 긋고 싶은 생각이 들어서 자해 충동이 있어요.");

        // then
        assertThat(response.getIsRisky()).isTrue();
        assertThat(response.getRiskScore()).isEqualTo(90);
        assertThat(response.getRiskType()).isEqualTo("SELF_HARM");
        assertThat(response.getAnalysisMethod()).isEqualTo("KEYWORD");
        assertThat(response.getDetectedKeywords()).isNotEmpty();
    }

    @Test
    @DisplayName("기타 고위험 키워드는 CRISIS로 판단한다")
    void analyze_returnsCrisisRisk_whenGenericHighRiskKeywordExists() {
        // when
        RiskScoreResponse response = keywordRiskAnalyzer.analyze("이 삶을 끝내고 싶다는 생각이 계속 들어요.");

        // then
        assertThat(response.getIsRisky()).isTrue();
        assertThat(response.getRiskScore()).isEqualTo(90);
        assertThat(response.getRiskType()).isEqualTo("CRISIS");
        assertThat(response.getAnalysisMethod()).isEqualTo("KEYWORD");
    }

    @Test
    @DisplayName("중위험 키워드가 두 개 이상이면 위험으로 판단한다")
    void analyze_returnsRisky_whenTwoOrMoreMediumRiskKeywordsExist() {
        // when
        RiskScoreResponse response = keywordRiskAnalyzer.analyze("요즘 희망이 없고 사라지고 싶다는 생각이 들어요.");

        // then
        assertThat(response.getIsRisky()).isTrue();
        assertThat(response.getRiskScore()).isEqualTo(60);
        assertThat(response.getRiskType()).isEqualTo("CRISIS");
        assertThat(response.getAnalysisMethod()).isEqualTo("KEYWORD");
        assertThat(response.getRecommendedAction()).isEqualTo("SHOW_SAFETY_MESSAGE");
        assertThat(response.getDetectedKeywords()).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("중위험 키워드가 하나만 있으면 모니터링 대상으로만 처리한다")
    void analyze_returnsNotRisky_whenOnlyOneMediumRiskKeywordExists() {
        // when
        RiskScoreResponse response = keywordRiskAnalyzer.analyze("요즘 희망이 없다는 생각이 자주 들어요.");

        // then
        assertThat(response.getIsRisky()).isFalse();
        assertThat(response.getRiskScore()).isEqualTo(60);
        assertThat(response.getRiskType()).isNull();
        assertThat(response.getAnalysisMethod()).isEqualTo("KEYWORD");
        assertThat(response.getRecommendedAction()).isEqualTo("MONITOR");
        assertThat(response.getDetectedKeywords()).hasSize(1);
    }

    @Test
    @DisplayName("위험 키워드가 없으면 safe 응답을 반환한다")
    void analyze_returnsSafe_whenNoKeywordExists() {
        // when
        RiskScoreResponse response = keywordRiskAnalyzer.analyze("오늘은 산책을 하면서 조금 차분해졌어요.");

        // then
        assertThat(response.getIsRisky()).isFalse();
        assertThat(response.getRiskScore()).isZero();
        assertThat(response.getRiskType()).isNull();
        assertThat(response.getDetectedKeywords()).isEmpty();
        assertThat(response.getAnalysisMethod()).isEqualTo("KEYWORD");
        assertThat(response.getRecommendedAction()).isEqualTo("NONE");
    }

    @Test
    @DisplayName("null 또는 blank 입력이면 safe 응답을 반환한다")
    void analyze_returnsSafe_whenContentIsNullOrBlank() {
        // when
        RiskScoreResponse nullResponse = keywordRiskAnalyzer.analyze(null);
        RiskScoreResponse blankResponse = keywordRiskAnalyzer.analyze("   ");

        // then
        assertThat(nullResponse.getIsRisky()).isFalse();
        assertThat(nullResponse.getRiskScore()).isZero();
        assertThat(blankResponse.getIsRisky()).isFalse();
        assertThat(blankResponse.getRiskScore()).isZero();
        assertThat(blankResponse.getAnalysisMethod()).isEqualTo("KEYWORD");
    }
}
