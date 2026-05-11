"use client";

import { useState, useEffect, useCallback } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { ChevronLeft, Search } from "lucide-react";
import { api } from "@/lib/api";

interface DiaryItem {
    id: number;
    title: string;
    writtenAt: string;
    primaryEmotion: string | null;
}

const EMOTION_EMOJI: Record<string, string> = {
    happy: "😊", calm: "😌", anxious: "😰",
    sad: "😢", angry: "😠", tired: "😴",
};

function formatDate(dateTimeStr: string): string {
    return new Date(dateTimeStr).toLocaleDateString("ko-KR", {
        year: "numeric", month: "long", day: "numeric",
    });
}

export default function DiarySearchPage() {
    const router = useRouter();
    const searchParams = useSearchParams();
    const initialQuery = searchParams.get("q") ?? "";

    const [inputValue, setInputValue] = useState(initialQuery);
    const [keyword, setKeyword] = useState(initialQuery);
    const [diaries, setDiaries] = useState<DiaryItem[]>([]);
    const [isLoading, setIsLoading] = useState(false);
    const [searched, setSearched] = useState(false);

    const handleSearch = useCallback(async (q: string) => {
        if (!q.trim()) return;
        setIsLoading(true);
        setSearched(true);
        try {
            const result = await api.get<{ content: DiaryItem[] }>(
                `/api/v1/diaries?keyword=${encodeURIComponent(q.trim())}&size=50`
            );
            setDiaries(result?.content ?? []);
        } catch {
            setDiaries([]);
        } finally {
            setIsLoading(false);
        }
    }, []);

    // URL에 q= 파라미터가 있으면 자동 검색
    useEffect(() => {
        if (initialQuery) handleSearch(initialQuery);
    }, [initialQuery, handleSearch]);

    function onSubmit(e: React.FormEvent<HTMLFormElement>) {
        e.preventDefault();
        setKeyword(inputValue);
        router.replace(`/diary/search?q=${encodeURIComponent(inputValue.trim())}`);
        handleSearch(inputValue);
    }

    return (
        <div className="p-4 md:p-8 max-w-2xl mx-auto w-full">
        {/* 헤더 */}
    <div className="flex items-center gap-2 mb-6">
        <Button variant="ghost" size="icon" onClick={() => router.back()}>
            <ChevronLeft className="w-5 h-5" />
        </Button>
        <h2 className="text-lg font-semibold text-gray-800">일기 검색</h2>
    </div>

    {/* 검색창 */}
    <form onSubmit={onSubmit} className="flex gap-2 mb-6">
        <input
            type="text"
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            placeholder="제목 또는 내용으로 검색"
            className="flex-1 border border-gray-200 rounded-lg px-3 py-2 text-sm outline-none focus:ring-2
  focus:ring-[#4A8EF0]"
        />
        <Button type="submit" className="bg-[#4A8EF0] hover:bg-[#3a7ee0]">
            <Search className="w-4 h-4" />
        </Button>
    </form>

    {/* 결과 */}
    {isLoading ? (
        <div className="text-center text-gray-400 text-sm py-20">검색 중...</div>
    ) : searched && diaries.length === 0 ? (
        <div className="text-center text-gray-400 text-sm py-20">
            &quot;{keyword}&quot; 에 해당하는 일기가 없어요.
        </div>
    ) : (
        <div className="flex flex-col gap-3">
            {searched && (
                <p className="text-xs text-gray-400 mb-1">
                    &quot;{keyword}&quot; 검색 결과 {diaries.length}개
                </p>
            )}
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
                                <p className="text-xs text-gray-400 mt-1">
                                    {formatDate(diary.writtenAt)}
                                </p>
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