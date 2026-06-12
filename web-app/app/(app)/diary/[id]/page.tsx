"use client";
// 파일: app/(app)/diary/[id]/page.tsx
// 역할: 일기 상세 + AI 분석 ("오늘의 기록 분석")
// 호출: GET /api/v1/diaries/{id}, DELETE /api/v1/diaries/{id} → backend-api
// 비고: 디자인 Figma 기준 폴리시. fetch/delete/edit 로직은 무수정.

import { useState, useEffect } from "react";
import { useRouter, useParams } from "next/navigation";
import { ChevronLeft, MoreHorizontal, Pencil, Trash2 } from "lucide-react";
import { api } from "@/lib/api";

// --- 타입 정의 ---

interface AiAnalysis {
    primaryEmotion: string | null;
    emotionIntensity: number | null;
    summary: string | null;
    confidence: number | null;
    riskLevel: string | null;
    recommendedAction: string | null;
}

interface DiaryDetail {
    id: number;
    title: string;
    content: string;
    writtenAt: string;
    primaryEmotion: string | null;
    emotionIntensity: number | null;
    aiAnalysis: AiAnalysis | null;
    createdAt: string;
    updatedAt: string;
}

// --- 상수 ---

const EMOTION_LABEL: Record<string, string> = {
    happy: "기쁨", calm: "평온", anxious: "불안",
    sad: "슬픔", angry: "화남", tired: "피곤",
};
const EMOTION_COLOR: Record<string, string> = {
    happy: "#FBBF24", calm: "#34D399", anxious: "#C77DD6",
    sad: "#60A5FA", angry: "#F87171", tired: "#9CA3AF",
};

// 위험도 → 주의 신호 카드 스타일/라벨 (채팅 안전 분기 명칭과 정렬)
const RISK_META: Record<string, { label: string; cardBg: string; titleColor: string }> = {
    LOW:    { label: "안정 · NORMAL RESPONSE",      cardBg: "bg-[#EAF7EF] border-[#BFE6CE]", titleColor: "text-[#2F855A]" },
    MEDIUM: { label: "중위험 · SUPPORTIVE RESPONSE", cardBg: "bg-[#FEF6E0] border-[#F6E2A8]", titleColor: "text-[#B7791F]" },
    HIGH:   { label: "고위험 · SAFETY RESPONSE",     cardBg: "bg-[#FDECEC] border-[#F6BDBD]", titleColor: "text-[#C53030]" },
};

// "2024-05-18T..." → "2024년 5월 18일 화요일"
const DOW = ["일", "월", "화", "수", "목", "금", "토"];
function formatKoreanDate(dateStr: string): string {
    const d = new Date(dateStr);
    return `${d.getFullYear()}년 ${d.getMonth() + 1}월 ${d.getDate()}일 ${DOW[d.getDay()]}요일`;
}
function toPercent(conf: number): number {
    return Math.round(conf <= 1 ? conf * 100 : conf);
}

// --- 컴포넌트 ---

export default function DiaryDetailPage() {
    const router = useRouter();
    const params = useParams();
    const id = params.id as string;

    const [diary, setDiary] = useState<DiaryDetail | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [isDeleting, setIsDeleting] = useState(false);
    const [menuOpen, setMenuOpen] = useState(false);

    useEffect(() => {
        async function fetchDiary() {
            try {
                const result = await api.get<DiaryDetail>(`/api/v1/diaries/${id}`);
                setDiary(result);
            } catch {
                router.push("/calendar");
            } finally {
                setIsLoading(false);
            }
        }
        fetchDiary();
    }, [id]);

    async function handleDelete() {
        setMenuOpen(false);
        if (!window.confirm("일기를 삭제할까요?")) return;
        setIsDeleting(true);
        try {
            await api.delete(`/api/v1/diaries/${id}`);
            router.push(`/diary?date=${diary?.writtenAt.slice(0, 10) ?? ""}`);
        } catch {
            setIsDeleting(false);
        }
    }

    if (isLoading) {
        return (
            <div className="flex-1 flex items-center justify-center text-gray-400 text-sm">
                불러오는 중...
            </div>
        );
    }
    if (!diary) return null;

    const ai = diary.aiAnalysis;
    const emotion = ai?.primaryEmotion ?? diary.primaryEmotion;
    const intensity = diary.emotionIntensity ?? ai?.emotionIntensity ?? null;
    const risk = ai?.riskLevel ? RISK_META[ai.riskLevel] : null;

    return (
        <div className="p-4 md:p-8 max-w-2xl mx-auto w-full pb-28">

            {/* 헤더 */}
            <div className="relative flex items-center justify-center mb-6 h-8">
                <button onClick={() => router.back()} className="absolute left-0 text-gray-700 hover:text-gray-900" aria-label="뒤로">
                    <ChevronLeft className="w-6 h-6" />
                </button>
                <h1 className="text-lg font-bold text-gray-800">오늘의 기록 분석</h1>
                <div className="absolute right-0">
                    <button onClick={() => setMenuOpen((v) => !v)} className="text-gray-400 hover:text-gray-700" aria-label="더보기">
                        <MoreHorizontal className="w-6 h-6" />
                    </button>
                    {menuOpen && (
                        <>
                            <div className="fixed inset-0 z-10" onClick={() => setMenuOpen(false)} />
                            <div className="absolute right-0 mt-1 w-32 bg-white rounded-xl shadow-lg ring-1 ring-gray-100 py-1 z-20">
                                <button
                                    onClick={() => router.push(`/diary/${id}/edit`)}
                                    className="flex items-center gap-2 w-full px-3 py-2 text-sm text-gray-700 hover:bg-gray-50"
                                >
                                    <Pencil className="w-4 h-4" /> 수정하기
                                </button>
                                <button
                                    onClick={handleDelete}
                                    disabled={isDeleting}
                                    className="flex items-center gap-2 w-full px-3 py-2 text-sm text-red-500 hover:bg-red-50"
                                >
                                    <Trash2 className="w-4 h-4" /> 삭제하기
                                </button>
                            </div>
                        </>
                    )}
                </div>
            </div>

            {/* 날짜 + 제목 */}
            <p className="text-sm font-medium text-gray-400">{formatKoreanDate(diary.writtenAt)}</p>
            <h2 className="text-2xl font-bold text-gray-800 mt-1 mb-5">{diary.title}</h2>

            {/* 내용 카드 + 감정 태그 */}
            <div className="rounded-2xl bg-gray-50 p-5">
                <p className="text-gray-700 whitespace-pre-wrap leading-relaxed">{diary.content}</p>
                {emotion && (
                    <div className="flex flex-wrap gap-2 mt-4">
                        <span
                            className="text-xs font-semibold rounded-full px-3 py-1.5"
                            style={{ backgroundColor: `${EMOTION_COLOR[emotion] ?? "#9CA3AF"}22`, color: EMOTION_COLOR[emotion] ?? "#6B7280" }}
                        >
                            {EMOTION_LABEL[emotion] ?? emotion}
                        </span>
                    </div>
                )}
            </div>

            {/* AI 감정 분석 카드 */}
            <div className="rounded-2xl bg-[#EEF3FE] p-5 mt-4">
                <div className="flex items-center justify-between">
                    <h3 className="text-lg font-bold text-gray-800">AI 감정 분석</h3>
                    {ai?.confidence != null && (
                        <span className="text-xs font-semibold rounded-full px-3 py-1.5 bg-[#34D399]/20 text-[#2F855A]">
                            신뢰도 {toPercent(ai.confidence)}%
                        </span>
                    )}
                </div>
                {emotion && (
                    <p className="text-sm font-medium text-gray-500 mt-1">대표 감정 · {emotion.toUpperCase()}</p>
                )}
                <p className="text-sm text-gray-600 leading-relaxed mt-3">
                    {ai?.summary ?? "분석 중이거나 아직 분석 결과가 없습니다."}
                </p>
            </div>

            {/* 주의 신호 카드 (위험도 있을 때) */}
            {risk && (
                <div className={`rounded-2xl border p-5 mt-4 ${risk.cardBg}`}>
                    <h3 className="text-lg font-bold text-gray-800">주의 신호</h3>
                    <p className={`text-sm font-bold mt-1 ${risk.titleColor}`}>{risk.label}</p>
                    {ai?.recommendedAction && (
                        <p className="text-sm text-gray-600 leading-relaxed mt-2">{ai.recommendedAction}</p>
                    )}
                </div>
            )}

            {/* 감정 강도 (읽기 전용) */}
            {intensity != null && (
                <div className="rounded-2xl bg-white ring-1 ring-gray-100 p-5 mt-4">
                    <h3 className="text-base font-bold text-gray-800 mb-3">감정 강도</h3>
                    <div className="flex items-center gap-4">
                        <input
                            type="range" min={1} max={5} step={1} value={intensity} readOnly disabled
                            className="flex-1 accent-[#4A8EF0]"
                        />
                        <span className="text-sm font-bold text-[#4A8EF0] shrink-0 w-10 text-right">{intensity} / 5</span>
                    </div>
                </div>
            )}

            {/* 하단 버튼 */}
            <div className="flex flex-col gap-3 mt-6">
                <button
                    onClick={() => router.push("/chat")}
                    className="w-full rounded-full bg-[#4A8EF0] hover:bg-[#3a7ee0] text-white font-bold py-4 text-base transition-colors"
                >
                    AI와 대화 이어가기
                </button>
                <button
                    onClick={() => router.push(`/diary/${id}/edit`)}
                    className="w-full rounded-full border border-[#4A8EF0] text-[#4A8EF0] font-semibold py-4 bg-white hover:bg-[#4A8EF0]/5 transition-colors"
                >
                    기록 수정하기
                </button>
            </div>
        </div>
    );
}
