"use client";
// 파일: app/(app)/diary/[id]/page.tsx
// 역할: 일기 상세 페이지 + AI 분석 결과 표시 (protected)
// 호출: GET /diaries/{id} → backend-api

import { useParams } from "next/navigation";

export default function DiaryDetailPage() {
  const params = useParams();
  const diaryId = params.id;

  return (
    <div className="flex flex-col p-4 md:p-8 max-w-2xl mx-auto w-full">
      <header className="mb-6">
        <h1 className="text-2xl font-bold text-gray-800">일기 상세</h1>
        <p className="text-sm text-gray-400 mt-1">ID: {diaryId}</p>
      </header>

      {/* 일기 내용 */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 mb-4">
        <div className="flex items-center justify-between mb-4">
          <span className="text-sm font-medium text-gray-500">2026년 5월 4일</span>
          <button className="text-sm text-gray-400 hover:text-gray-600">수정</button>
        </div>
        <p className="text-gray-700 leading-relaxed">
          일기 내용이 여기에 표시됩니다...
        </p>
      </div>

      {/* AI 분석 결과 */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6">
        <h2 className="text-base font-semibold text-gray-800 mb-4">AI 분석 결과</h2>

        {/* 감정 분석 */}
        <div className="mb-4">
          <h3 className="text-sm font-medium text-gray-600 mb-2">감정 분석</h3>
          <div className="flex flex-wrap gap-2">
            <span className="px-3 py-1 rounded-full text-xs font-medium bg-[#F472B6]/10 text-[#F472B6]">
              불안 45%
            </span>
            <span className="px-3 py-1 rounded-full text-xs font-medium bg-[#60A5FA]/10 text-[#60A5FA]">
              걱정 30%
            </span>
            <span className="px-3 py-1 rounded-full text-xs font-medium bg-[#34D399]/10 text-[#34D399]">
              안정 25%
            </span>
          </div>
        </div>

        {/* AI 요약 */}
        <div className="mb-4">
          <h3 className="text-sm font-medium text-gray-600 mb-2">AI 요약</h3>
          <p className="text-sm text-gray-500 bg-gray-50 rounded-lg p-3">
            AI 분석 요약이 여기에 표시됩니다...
          </p>
        </div>

        {/* 위험도 */}
        <div>
          <h3 className="text-sm font-medium text-gray-600 mb-2">위험도</h3>
          <span className="px-3 py-1 rounded-full text-xs font-medium bg-green-100 text-green-600">
            LOW
          </span>
        </div>
      </div>
    </div>
  );
}
