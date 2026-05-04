"use client";
// 파일: app/(app)/diary/new/page.tsx
// 역할: 일기 쓰기 페이지 (protected)
// 호출: POST /diaries → backend-api → ai-api (감정 분석, 위험도)

export default function DiaryNewPage() {
  return (
    <div className="flex flex-col p-4 md:p-8 max-w-2xl mx-auto w-full">
      <header className="mb-6">
        <h1 className="text-2xl font-bold text-gray-800">일기 쓰기</h1>
        <p className="text-sm text-gray-400 mt-1">오늘의 감정을 기록해보세요</p>
      </header>

      <form className="flex flex-col gap-4">
        {/* 날짜 */}
        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4">
          <label className="block text-sm font-medium text-gray-700 mb-2">
            날짜
          </label>
          <input
            type="date"
            className="w-full px-4 py-2.5 border border-gray-200 rounded-lg text-sm focus:outline-none focus:border-[#4A8EF0]"
          />
        </div>

        {/* 감정 선택 */}
        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4">
          <label className="block text-sm font-medium text-gray-700 mb-3">
            오늘의 감정
          </label>
          <div className="flex flex-wrap gap-2">
            {[
              { label: "불안", color: "#F472B6" },
              { label: "걱정", color: "#60A5FA" },
              { label: "안정", color: "#34D399" },
              { label: "기쁨", color: "#FBBF24" },
              { label: "무기력", color: "#9CA3AF" },
            ].map((emotion) => (
              <button
                key={emotion.label}
                type="button"
                className="px-4 py-2 rounded-full text-sm font-medium border-2 transition-all"
                style={{ borderColor: emotion.color, color: emotion.color }}
              >
                {emotion.label}
              </button>
            ))}
          </div>
        </div>

        {/* 일기 내용 */}
        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4">
          <label className="block text-sm font-medium text-gray-700 mb-2">
            오늘 있었던 일
          </label>
          <textarea
            rows={8}
            placeholder="오늘 하루를 자유롭게 적어보세요..."
            className="w-full px-4 py-3 border border-gray-200 rounded-lg text-sm focus:outline-none focus:border-[#4A8EF0] resize-none"
          />
        </div>

        <button
          type="submit"
          className="w-full py-3 bg-[#4A8EF0] text-white font-medium rounded-xl hover:bg-[#3a7ee0] transition-colors"
        >
          저장하기
        </button>
      </form>
    </div>
  );
}
