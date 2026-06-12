"use client";
export const dynamic = "force-dynamic";
// 파일: app/(app)/diary/page.tsx
// 역할: 날짜별 일기 목록 페이지
// 호출: GET /api/v1/calendar/{date} → 해당 날짜의 일기 목록
// 비고: 디자인 언어(둥근 카드/헤더/알약 버튼) 일관성 폴리시. fetch 로직 보존.

import { useState, useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { ChevronLeft, Plus } from "lucide-react";
import { api } from "@/lib/api";

interface DiaryItem {
    id: number;
    title: string;
    writtenAt: string;
    primaryEmotion: string | null;
    emotionIntensity: number | null;
}

const EMOTION_EMOJI: Record<string, string> = {
    happy: "😊", calm: "😌", anxious: "😰",
    sad: "😢", angry: "😠", tired: "😴",
};
const EMOTION_LABEL: Record<string, string> = {
    happy: "기쁨", calm: "평온", anxious: "불안",
    sad: "슬픔", angry: "화남", tired: "피곤",
};

function formatDate(dateStr: string): string {
    return new Date(dateStr + "T00:00:00").toLocaleDateString("ko-KR", {
        year: "numeric", month: "long", day: "numeric",
    });
}
function formatTime(dateTimeStr: string): string {
    return new Date(dateTimeStr).toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit" });
}

const MAX_DAILY = 3;

export default function DiaryListPage() {
    const router = useRouter();
    const searchParams = useSearchParams();
    const date = searchParams.get("date") ?? new Date().toISOString().slice(0, 10);

    const [diaries, setDiaries] = useState<DiaryItem[]>([]);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        async function fetchDiaries() {
            setIsLoading(true);
            try {
                const result = await api.get<DiaryItem[]>(`/api/v1/calendar/${date}`);
                setDiaries(result ?? []);
            } catch {
                setDiaries([]);
            } finally {
                setIsLoading(false);
            }
        }
        fetchDiaries();
    }, [date]);

    const isFull = diaries.length >= MAX_DAILY;

    return (
        <div className="p-4 md:p-8 max-w-2xl mx-auto w-full pb-28">

            {/* 헤더 */}
            <div className="relative flex items-center justify-center mb-6 h-10">
                <button onClick={() => router.back()} className="absolute left-0 text-gray-700 hover:text-gray-900" aria-label="뒤로">
                    <ChevronLeft className="w-6 h-6" />
                </button>
                <div className="text-center">
                    <h1 className="text-base font-bold text-gray-800">{formatDate(date)}</h1>
                    <p className="text-xs text-gray-400">{diaries.length} / {MAX_DAILY}개</p>
                </div>
            </div>

            {/* 목록 */}
            {isLoading ? (
                <div className="flex items-center justify-center h-48 text-gray-400 text-sm">불러오는 중...</div>
            ) : diaries.length === 0 ? (
                <div className="flex flex-col items-center justify-center h-48 gap-4">
                    <p className="text-gray-400 text-sm">이 날의 일기가 없어요.</p>
                    <button
                        onClick={() => router.push(`/diary/new?date=${date}`)}
                        className="rounded-full bg-[#4A8EF0] hover:bg-[#3a7ee0] text-white font-semibold px-6 py-3 transition-colors"
                    >
                        일기 쓰기
                    </button>
                </div>
            ) : (
                <div className="flex flex-col gap-3">
                    {diaries.map((diary) => (
                        <button
                            key={diary.id}
                            onClick={() => router.push(`/diary/${diary.id}`)}
                            className="w-full text-left rounded-2xl bg-white ring-1 ring-gray-100 shadow-sm p-4 hover:bg-gray-50 transition-colors"
                        >
                            <div className="flex items-start gap-3">
                                <span className="text-2xl mt-0.5">
                                    {diary.primaryEmotion ? (EMOTION_EMOJI[diary.primaryEmotion] ?? "📔") : "📔"}
                                </span>
                                <div className="flex-1 min-w-0">
                                    <p className="text-sm font-semibold text-gray-800 truncate">{diary.title}</p>
                                    <div className="flex items-center gap-2 mt-1">
                                        <span className="text-xs text-gray-400">{formatTime(diary.writtenAt)}</span>
                                        {diary.primaryEmotion && (
                                            <span className="text-xs text-gray-400">· {EMOTION_LABEL[diary.primaryEmotion] ?? diary.primaryEmotion}</span>
                                        )}
                                    </div>
                                </div>
                            </div>
                        </button>
                    ))}
                </div>
            )}

            {/* 플로팅 작성 버튼 (가득 차지 않았을 때) */}
            {!isFull && (
                <button
                    onClick={() => router.push(`/diary/new?date=${date}`)}
                    aria-label="일기 작성"
                    className="fixed bottom-20 right-5 md:bottom-8 w-14 h-14 rounded-full bg-[#4A8EF0] hover:bg-[#3a7ee0] text-white shadow-lg flex items-center justify-center z-40 transition-colors"
                >
                    <Plus className="w-7 h-7" />
                </button>
            )}
        </div>
    );
}
