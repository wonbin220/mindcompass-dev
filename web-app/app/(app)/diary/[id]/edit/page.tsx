"use client";
// 파일: app/(app)/diary/[id]/edit/page.tsx
// 역할: 일기 수정 페이지
// 호출: GET /api/v1/diaries/{id} → 기존 데이터 로드
//       PATCH /api/v1/diaries/{id} → 수정 저장

import { useState, useEffect } from "react";
import { useRouter, useParams } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Card, CardContent } from "@/components/ui/card";
import { api } from "@/lib/api";

const EMOTIONS = [
    { code: "happy",   emoji: "😊", label: "기쁨" },
    { code: "calm",    emoji: "😌", label: "평온" },
    { code: "anxious", emoji: "😰", label: "불안" },
    { code: "sad",     emoji: "😢", label: "슬픔" },
    { code: "angry",   emoji: "😠", label: "화남" },
    { code: "tired",   emoji: "😴", label: "피곤" },
];
const INTENSITIES = [1, 2, 3, 4, 5];

interface DiaryDetail {
    id: number;
    title: string;
    content: string;
    writtenAt: string; // "2024-05-15T09:30:00"
    primaryEmotion?: string | null;
    emotionIntensity?: number | null;
}

// "2024-05-15T09:30:00" → date: "2024-05-15", time: "09:30"
function splitDateTime(dateTimeStr: string) {
    const [date, timeFull] = dateTimeStr.split("T");
    const time = timeFull.slice(0, 5); // "09:30:00" → "09:30"
    return { date, time };
}

function toLocalDateTime(date: string, time: string): string {
    return `${date}T${time}:00`;
}

export default function DiaryEditPage() {
    const router = useRouter();
    const params = useParams();
    const id = params.id as string;

    const [title, setTitle] = useState("");
    const [content, setContent] = useState("");
    const [date, setDate] = useState("");
    const [time, setTime] = useState("");
    const [isLoading, setIsLoading] = useState(true);  // 초기 fetch 로딩
    const [isSaving, setIsSaving] = useState(false);   // 저장 로딩
    const [errorMessage, setErrorMessage] = useState("");
    const [selectedEmotion, setSelectedEmotion] = useState<string | null>(null);
    const [emotionIntensity, setEmotionIntensity] = useState<number | null>(null);

    // 마운트 시 기존 데이터 fetch → 폼에 채우기
    useEffect(() => {
        async function fetchDiary() {
            try {
                const result = await api.get<DiaryDetail>(`/api/v1/diaries/${id}`);
                const diary = result;
                // 기존 값을 state에 채워넣기
                setTitle(diary.title);
                setContent(diary.content);

                const { date, time } = splitDateTime(diary.writtenAt);
                setDate(date);
                setTime(time);

                setSelectedEmotion(diary.primaryEmotion ?? null);
                setEmotionIntensity(diary.emotionIntensity ?? null);

            } catch {
                router.push("/calendar");
            } finally {
                setIsLoading(false);
            }
        }
        fetchDiary();
    }, [id]);

    async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
        e.preventDefault();
        setErrorMessage("");

        if (!title.trim()) {
            setErrorMessage("제목을 입력해주세요.");
            return;
        }
        if (!content.trim()) {
            setErrorMessage("내용을 입력해주세요.");
            return;
        }

        setIsSaving(true);
        try {
            await api.patch(
                `/api/v1/diaries/${id}`,
                {
                    title,
                    content,
                    writtenAt: toLocalDateTime(date, time),
                    ...(selectedEmotion !== undefined && { primaryEmotion: selectedEmotion }),
                    ...(emotionIntensity !== undefined && { emotionIntensity }),
                }
            );
            router.push(`/diary/${id}`);
        } catch {
            setErrorMessage("저장에 실패했습니다. 다시 시도해주세요.");
        } finally {
            setIsSaving(false);
        }
    }

    if (isLoading) {
        return (
            <div className="flex-1 flex items-center justify-center text-gray-400 text-sm">
                불러오는 중...
            </div>
        );
    }

    return (
        <div className="p-4 md:p-8 max-w-2xl mx-auto w-full">
            <h2 className="text-lg font-semibold text-gray-800 mb-6">일기 수정</h2>

            <form onSubmit={handleSubmit} className="flex flex-col gap-6">
                <Card>
                    <CardContent className="pt-6 flex flex-col gap-4">

                        <div className="flex flex-col gap-1.5">
                            <Label htmlFor="title">제목</Label>
                            <Input
                                id="title"
                                value={title}
                                onChange={(e) => setTitle(e.target.value)}
                                maxLength={200}
                                required
                            />
                        </div>

                        <div className="flex gap-3">
                            <div className="flex flex-col gap-1.5 flex-1">
                                <Label htmlFor="date">날짜</Label>
                                <Input
                                    id="date"
                                    type="date"
                                    value={date}
                                    onChange={(e) => setDate(e.target.value)}
                                    required
                                />
                            </div>
                            <div className="flex flex-col gap-1.5 flex-1">
                                <Label htmlFor="time">시간</Label>
                                <Input
                                    id="time"
                                    type="time"
                                    value={time}
                                    onChange={(e) => setTime(e.target.value)}
                                    required
                                />
                            </div>
                        </div>
                        <div className="flex flex-col gap-1.5">
                            <Label htmlFor="content">내용</Label>
                            <Textarea
                                id="content"
                                value={content}
                                onChange={(e) => setContent(e.target.value)}
                                className="min-h-[200px] resize-none"
                                maxLength={10000}
                                required
                            />
                            <p className="text-xs text-gray-400 text-right">
                                {content.length} / 10,000
                            </p>
                        </div>

                    </CardContent>
                </Card>

                <Card>                                                                                                       [14/1859]
                    <CardContent className="pt-6 flex flex-col gap-4">
                        <p className="text-sm font-medium text-gray-700">
                            오늘의 감정 <span className="text-gray-400 font-normal">(선택)</span>
                        </p>
                        <div className="grid grid-cols-6 gap-2">
                            {EMOTIONS.map((em) => (
                                <button
                                    key={em.code}
                                    type="button"
                                    onClick={() => setSelectedEmotion(prev => prev === em.code ? null : em.code)}
                                    className={`flex flex-col items-center gap-1 py-2 rounded-lg border transition-colors 
                                    ${selectedEmotion === em.code
                                        ? "border-[#4A8EF0] bg-blue-50"
                                        : "border-gray-200 hover:bg-gray-50"}`}>
                                    <span className="text-xl">{em.emoji}</span>
                                    <span className="text-xs text-gray-600">{em.label}</span>
                                </button>
                            ))}
                        </div>
                        {selectedEmotion && (
                            <div className="flex gap-2">
                                {INTENSITIES.map((n) => (
                                    <button
                                        key={n}
                                        type="button"
                                        onClick={() => setEmotionIntensity(prev => prev === n ? null : n)}
                                        className={`flex-1 py-2 rounded-lg border text-sm font-medium transition-colors
                                        ${emotionIntensity === n
                                            ? "border-[#4A8EF0] bg-blue-50 text-[#4A8EF0]"
                                            : "border-gray-200 text-gray-600 hover:bg-gray-50"}`}>
                                        {n}
                                    </button>
                                ))}
                            </div>
                        )}
                    </CardContent>
                </Card>


                {errorMessage && (
                    <p className="text-red-500 text-sm">{errorMessage}</p>
                )}

                <div className="flex gap-3">
                    <Button
                        type="button"
                        variant="outline"
                        className="flex-1"
                        onClick={() => router.back()}
                    >
                        취소
                    </Button>
                    <Button
                        type="submit"
                        disabled={isSaving}
                        className="flex-1 bg-[#4A8EF0] hover:bg-[#3a7ee0]"
                    >
                        {isSaving ? "저장 중..." : "저장하기"}
                    </Button>
                </div>

            </form>
        </div>
    );
}