"use client";
// 파일: app/(app)/calendar/page.tsx
// 역할: 캘린더 홈 페이지 - 월별 감정 기록 달력 표시 (protected)
// 호출: GET /calendar/monthly-emotions, GET /calendar/daily-summary → backend-api

export default function CalendarPage() {
  return (
    <div className="flex flex-col p-4 md:p-8 max-w-2xl mx-auto w-full">
      <header className="mb-6">
        <h1 className="text-2xl font-bold text-gray-800">캘린더</h1>
        <p className="text-sm text-gray-400 mt-1">나의 감정 기록</p>
      </header>

      {/* 월 네비게이션 */}
      <div className="flex items-center justify-between mb-4">
        <button className="p-2 rounded-lg hover:bg-gray-100 text-gray-500">
          ←
        </button>
        <span className="font-semibold text-gray-700">2026년 5월</span>
        <button className="p-2 rounded-lg hover:bg-gray-100 text-gray-500">
          →
        </button>
      </div>

      {/* 캘린더 그리드 placeholder */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4">
        <div className="grid grid-cols-7 gap-1 mb-2">
          {["일", "월", "화", "수", "목", "금", "토"].map((day) => (
            <div
              key={day}
              className="text-center text-xs font-medium text-gray-400 py-1"
            >
              {day}
            </div>
          ))}
        </div>
        <div className="grid grid-cols-7 gap-1">
          {Array.from({ length: 35 }, (_, i) => (
            <div
              key={i}
              className="aspect-square flex items-center justify-center rounded-full text-sm text-gray-600 hover:bg-gray-50 cursor-pointer"
            >
              {i < 31 ? i + 1 : ""}
            </div>
          ))}
        </div>
      </div>

      {/* 감정 범례 */}
      <div className="mt-6 bg-white rounded-2xl shadow-sm border border-gray-100 p-4">
        <h2 className="text-sm font-semibold text-gray-700 mb-3">감정 색상</h2>
        <div className="flex flex-wrap gap-3">
          {[
            { label: "불안", color: "#F472B6" },
            { label: "걱정", color: "#60A5FA" },
            { label: "안정", color: "#34D399" },
            { label: "기쁨", color: "#FBBF24" },
            { label: "무기력", color: "#9CA3AF" },
          ].map((emotion) => (
            <div key={emotion.label} className="flex items-center gap-1.5">
              <div
                className="w-3 h-3 rounded-full"
                style={{ backgroundColor: emotion.color }}
              />
              <span className="text-xs text-gray-600">{emotion.label}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
