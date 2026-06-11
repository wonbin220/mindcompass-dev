"use client";
export const dynamic = "force-dynamic";
// 파일: app/(app)/diary/page.tsx
// 역할: 날짜별 일기 목록 페이지
// 호출: GET /api/v1/calendar/{date} → 해당 날짜의 일기 목록

import { useState, useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
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
    return new Date(dateTimeStr).toLocaleTimeString("ko-KR", {
        hour: "2-digit", minute: "2-digit",
    });
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
        <div className="p-4 md:p-8 max-w-2xl mx-auto w-full">

            {/* 헤더 */}
            <div className="flex items-center justify-between mb-6">
                <div className="flex items-center gap-2">
                    <Button variant="ghost" size="icon" onClick={() => router.back()}>
                        <ChevronLeft className="w-5 h-5" />
                    </Button>
                    <div>
                        <h2 className="text-lg font-semibold text-gray-800">
                            {formatDate(date)}
                        </h2>
                        <p className="text-xs text-gray-400">{diaries.length} / {MAX_DAILY}개</p>
                    </div>
                </div>

                {isFull ? (
                    <span className="text-xs text-gray-400">오늘의 기록이 가득 찼어요 ✨</span>
                ) : (
                    <Button
                        size="sm"
                        className="bg-[#4A8EF0] hover:bg-[#3a7ee0]"
                        onClick={() => router.push(`/diary/new?date=${date}`)}
                    >
                        <Plus className="w-4 h-4 mr-1" />
                        새로 쓰기
                    </Button>
                )}
            </div>

            {/* 목록 */}
            {isLoading ? (
                <div className="flex items-center justify-center h-48 text-gray-400 text-sm">
                    불러오는 중...
                </div>
            ) : diaries.length === 0 ? (
                <div className="flex flex-col items-center justify-center h-48 gap-4">
                    <p className="text-gray-400 text-sm">이 날의 일기가 없어요.</p>
                    <Button
                        className="bg-[#4A8EF0] hover:bg-[#3a7ee0]"
                        onClick={() => router.push(`/diary/new?date=${date}`)}
                    >
                        일기 쓰기
                    </Button>
                </div>
            ) : (
                <div className="flex flex-col gap-3">
                    {diaries.map((diary) => (
                        <Card
                            key={diary.id}
                            className="cursor-pointer hover:shadow-sm transition-shadow"
                            onClick={() => router.push(`/diary/${diary.id}`)}
                        >
                            <CardContent className="pt-4 pb-4">
                                <div className="flex items-start gap-3">
                                    <span className="text-2xl mt-0.5">
                                        {diary.primaryEmotion
                                            ? (EMOTION_EMOJI[diary.primaryEmotion] ?? "📔")
                                            : "📔"}
                                    </span>
                                    <div className="flex-1 min-w-0">
                                        <p className="text-sm font-medium text-gray-800 truncate">
                                            {diary.title}
                                        </p>
                                        <div className="flex items-center gap-2 mt-1">
                                            <span className="text-xs text-gray-400">
                                                {formatTime(diary.writtenAt)}
                                            </span>
                                            {diary.primaryEmotion && (
                                                <span className="text-xs text-gray-400">
                                                    · {EMOTION_LABEL[diary.primaryEmotion] ?? diary.primaryEmotion}
                                                </span>
                                            )}
                                        </div>
                                    </div>
                                </div>
                            </CardContent>
                        </Card>
                    ))}
                </div>
            )}
        </div>
    );
}
