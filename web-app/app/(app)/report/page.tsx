"use client";
// 파일: app/(app)/report/page.tsx
// 역할: 감정 리포트 페이지 - 주간/월간 감정 통계 (protected)
// 호출: GET /reports/monthly-summary, GET /reports/emotions/weekly → backend-api

export default function ReportPage() {
  return (
    <div className="flex flex-col p-4 md:p-8 max-w-2xl mx-auto w-full">
      <header className="mb-6">
        <h1 className="text-2xl font-bold text-gray-800">감정 리포트</h1>
        <p className="text-sm text-gray-400 mt-1">나의 감정 패턴 분석</p>
      </header>

      {/* 기간 선택 */}
      <div className="flex gap-2 mb-6">
        {["주간", "월간"].map((period) => (
          <button
            key={period}
            className="px-4 py-2 rounded-full text-sm font-medium bg-[#4A8EF0] text-white"
          >
            {period}
          </button>
        ))}
      </div>

      {/* 주간 감정 요약 */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 mb-4">
        <h2 className="text-base font-semibold text-gray-800 mb-4">이번 주 감정 분포</h2>
        <div className="flex flex-col gap-3">
          {[
            { label: "안정", color: "#34D399", percent: 40 },
            { label: "걱정", color: "#60A5FA", percent: 30 },
            { label: "불안", color: "#F472B6", percent: 20 },
            { label: "무기력", color: "#9CA3AF", percent: 10 },
          ].map((emotion) => (
            <div key={emotion.label} className="flex items-center gap-3">
              <span className="text-sm text-gray-600 w-12">{emotion.label}</span>
              <div className="flex-1 bg-gray-100 rounded-full h-2.5 overflow-hidden">
                <div
                  className="h-full rounded-full transition-all"
                  style={{
                    width: `${emotion.percent}%`,
                    backgroundColor: emotion.color,
                  }}
                />
              </div>
              <span className="text-sm text-gray-400 w-10 text-right">
                {emotion.percent}%
              </span>
            </div>
          ))}
        </div>
      </div>

      {/* 월간 위험도 */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 mb-4">
        <h2 className="text-base font-semibold text-gray-800 mb-4">이번 달 위험도 추이</h2>
        <div className="h-24 bg-gray-50 rounded-lg flex items-center justify-center">
          <p className="text-sm text-gray-400">차트 영역 (구현 예정)</p>
        </div>
      </div>

      {/* AI 리포트 요약 */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6">
        <h2 className="text-base font-semibold text-gray-800 mb-3">AI 종합 분석</h2>
        <p className="text-sm text-gray-500 bg-gray-50 rounded-lg p-4 leading-relaxed">
          이번 주 전반적으로 안정적인 감정 상태를 유지했습니다. AI 분석 내용이 여기에 표시됩니다...
        </p>
      </div>
    </div>
  );
}
