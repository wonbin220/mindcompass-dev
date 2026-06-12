"use client";
// 파일: app/(app)/analysis/page.tsx
// 역할: '오늘의 기록 분석' 화면 (Figma 신규 화면)
//   - 일기 요약 + AI 감정 타임라인 + 대표 감정 선택 + 마음 온도 + 행동 버튼
// 비고: 프론트 목업, 백엔드 무연동. 타임라인/대표감정/마음온도는 하드코딩 목업 데이터.

import { useRouter } from "next/navigation";
import { ChevronLeft } from "lucide-react";

// AI 감정 타임라인 (목업) — 하루 시간대별 감정 변화
const TIMELINE = [
    { time: "오전 09:12", label: "긴장/불안", inner: "#C77DD6", outer: "#E9C8F0", text: "text-[#C77DD6]", bold: false },
    { time: "오후 14:30", label: "뿌듯함", inner: "#FBBF24", outer: "#FDE9B5", text: "text-[#F59E0B]", bold: true },
    { time: "오후 20:05", label: "차분함", inner: "#5FD68A", outer: "#C4EFD3", text: "text-[#34D399]", bold: false },
];

export default function AnalysisPage() {
    const router = useRouter();

    return (
        <div className="p-4 md:p-8 max-w-2xl mx-auto w-full">

            {/* 헤더 — chevron 좌측, 제목 가운데 */}
            <div className="relative flex items-center justify-center mb-5 h-8">
                <button
                    onClick={() => router.back()}
                    className="absolute left-0 text-gray-700 hover:text-gray-900"
                    aria-label="뒤로"
                >
                    <ChevronLeft className="w-6 h-6" />
                </button>
                <h1 className="text-lg font-bold text-gray-800">오늘의 기록 분석</h1>
            </div>

            {/* 일기 요약 카드 */}
            <div className="rounded-2xl bg-white ring-1 ring-gray-100 p-5">
                <p className="font-bold text-gray-800">&quot;오전엔 발표 때문에 떨렸지만...&quot;</p>
                <p className="text-gray-500 text-sm leading-relaxed mt-2">
                    오늘은 중요한 발표가 있었다. 아침부터 심장이 두근거려 힘들었지만, 오후에 동료들의 칭찬을 듣고 기분이 좋아졌다. 하지만 퇴근길에...
                </p>
            </div>

            {/* AI 감정 타임라인 */}
            <h2 className="text-base font-bold text-gray-800 mt-6 mb-4">AI 감정 타임라인</h2>
            <div className="relative px-2">
                {/* 노드 사이 가로 연결선 */}
                <div className="absolute top-6 left-8 right-8 h-0.5 bg-gray-200 z-0" />
                <div className="relative z-10 flex items-start justify-between">
                    {TIMELINE.map((node) => (
                        <div key={node.time} className="flex flex-col items-center">
                            <span
                                className="w-12 h-12 rounded-full"
                                style={{ backgroundColor: node.inner, boxShadow: `0 0 0 5px ${node.outer}` }}
                            />
                            <span className="text-xs text-gray-500 mt-3">{node.time}</span>
                            <span className={`text-xs mt-0.5 ${node.text} ${node.bold ? "font-bold" : "font-medium"}`}>
                                {node.label}
                            </span>
                        </div>
                    ))}
                </div>
            </div>

            {/* 대표 감정 선택 카드 */}
            <div className="rounded-2xl bg-[#EEF3FE] p-4 mt-8">
                <p className="text-[#4A8EF0] font-bold">AI가 분석한 오늘의 대표 감정은?</p>
                <div className="flex gap-2 mt-3">
                    <button className="bg-[#FBBF24] text-white rounded-full px-5 py-2 font-medium">기쁨</button>
                    <button className="border border-gray-300 text-gray-400 rounded-full px-5 py-2 bg-white">수정하기</button>
                </div>
            </div>

            {/* 마음 온도 말풍선 */}
            <div className="flex justify-end mt-3">
                <span className="bg-[#4A8EF0] text-white text-sm rounded-2xl px-4 py-2">
                    마음 온도가 높아질 수 있어요!
                </span>
            </div>

            {/* 행동 버튼 */}
            <div className="flex flex-col gap-3 mt-6">
                <button
                    onClick={() => router.push("/chat")}
                    className="w-full bg-[#4A8EF0] hover:bg-[#3a7ee0] text-white font-bold rounded-full py-4 text-base transition-colors"
                >
                    AI 상담사와 이야기하기
                </button>
                <button
                    onClick={() => router.push("/calendar")}
                    className="w-full border border-[#4A8EF0] text-[#4A8EF0] rounded-full py-4 bg-white hover:bg-[#4A8EF0]/5 transition-colors"
                >
                    일기만 저장할게요
                </button>
            </div>
        </div>
    );
}
