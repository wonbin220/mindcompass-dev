"use client";
// 파일: app/(app)/diary/[id]/edit/page.tsx
// 역할: 일기 수정 페이지 ("기록 수정")
// 호출: GET /api/v1/diaries/{id} (기존 로드), PATCH /api/v1/diaries/{id} (저장)
// 비고: 디자인은 일기 쓰기와 동일 언어. fetch/PATCH 로직 보존.
//       PATCH는 title/content/writtenAt를 함께 보낸다(누락 시 내용 유실 방지).
//       날짜/시간은 UI 없이 기존 writtenAt 값을 유지해 그대로 전송.

import { useState, useEffect } from "react";
import { useRouter, useParams } from "next/navigation";
import { ChevronLeft } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { api } from "@/lib/api";

const EMOTIONS = [
    { code: "happy",   emoji: "😊", label: "기쁨", color: "#FBBF24" },
    { code: "calm",    emoji: "😌", label: "평온", color: "#34D399" },
    { code: "anxious", emoji: "😰", label: "불안", color: "#C77DD6" },
    { code: "sad",     emoji: "😢", label: "슬픔", color: "#60A5FA" },
    { code: "angry",   emoji: "😠", label: "화남", color: "#F87171" },
    { code: "tired",   emoji: "😴", label: "피곤", color: "#9CA3AF" },
];

const FIELD_CLASS =
    "rounded-xl border-0 bg-gray-100 px-4 text-base placeholder:text-gray-400 focus-visible:ring-2 focus-visible:ring-[#4A8EF0]/40";

interface DiaryDetail {
    id: number;
    title: string;
    content: string;
    writtenAt: string;
    primaryEmotion?: string | null;
    emotionIntensity?: number | null;
}

function splitDateTime(dateTimeStr: string) {
    const [date, timeFull] = dateTimeStr.split("T");
    return { date, time: timeFull.slice(0, 5) };
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
    // 날짜/시간은 UI 없이 기존 값 유지 (PATCH에 그대로 전송)
    const [date, setDate] = useState("");
    const [time, setTime] = useState("");
    const [isLoading, setIsLoading] = useState(true);
    const [isSaving, setIsSaving] = useState(false);
    const [errorMessage, setErrorMessage] = useState("");
    const [selectedEmotion, setSelectedEmotion] = useState<string | null>(null);
    const [emotionIntensity, setEmotionIntensity] = useState(3);

    useEffect(() => {
        async function fetchDiary() {
            try {
                const diary = await api.get<DiaryDetail>(`/api/v1/diaries/${id}`);
                setTitle(diary.title);
                setContent(diary.content);
                const { date, time } = splitDateTime(diary.writtenAt);
                setDate(date);
                setTime(time);
                setSelectedEmotion(diary.primaryEmotion ?? null);
                setEmotionIntensity(diary.emotionIntensity ?? 3);
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

        if (!title.trim()) { setErrorMessage("제목을 입력해주세요."); return; }
        if (!content.trim()) { setErrorMessage("내용을 입력해주세요."); return; }

        setIsSaving(true);
        try {
            // 누락 시 내용 유실 방지를 위해 title/content/writtenAt를 항상 함께 전송
            await api.patch(
                `/api/v1/diaries/${id}`,
                {
                    title,
                    content,
                    writtenAt: toLocalDateTime(date, time),
                    primaryEmotion: selectedEmotion,
                    emotionIntensity,
                }
            );
            router.push(`/diary/${id}`);
        } catch {
            setErrorMessage("저장에 실패했습니다. 다시 시도해주세요.");
        } finally {
            setIsSaving(false);
        }
    }

    function toggleEmotion(code: string) {
        setSelectedEmotion(prev => prev === code ? null : code);
    }

    if (isLoading) {
        return (
            <div className="flex-1 flex items-center justify-center text-gray-400 text-sm">불러오는 중...</div>
        );
    }

    return (
        <div className="p-4 md:p-8 max-w-2xl mx-auto w-full pb-28">

            {/* 헤더 */}
            <div className="relative flex items-center justify-center mb-5 h-8">
                <button onClick={() => router.back()} className="absolute left-0 text-gray-700 hover:text-gray-900" aria-label="뒤로">
                    <ChevronLeft className="w-6 h-6" />
                </button>
                <h1 className="text-lg font-bold text-gray-800">기록 수정</h1>
            </div>

            <form onSubmit={handleSubmit} className="flex flex-col gap-6">

                {/* 제목 */}
                <div className="flex flex-col gap-2">
                    <label htmlFor="title" className="text-sm font-semibold text-gray-800">제목</label>
                    <Input
                        id="title"
                        className={`h-14 ${FIELD_CLASS}`}
                        value={title}
                        onChange={(e) => setTitle(e.target.value)}
                        maxLength={200}
                        required
                    />
                </div>

                {/* 내용 */}
                <div className="flex flex-col gap-2">
                    <label htmlFor="content" className="text-sm font-semibold text-gray-800">내용</label>
                    <div className="relative">
                        <Textarea
                            id="content"
                            value={content}
                            onChange={(e) => setContent(e.target.value)}
                            className={`min-h-[200px] resize-none pb-7 ${FIELD_CLASS} py-4`}
                            maxLength={10000}
                            required
                        />
                        <span className="absolute bottom-3 right-4 text-xs text-gray-400">{content.length} / 10000</span>
                    </div>
                </div>

                {/* 대표 감정 */}
                <div className="flex flex-col gap-3">
                    <label className="text-sm font-semibold text-gray-800">
                        대표 감정 <span className="text-gray-400 font-normal">(선택)</span>
                    </label>
                    <div className="grid grid-cols-6 gap-2">
                        {EMOTIONS.map((em) => {
                            const active = selectedEmotion === em.code;
                            return (
                                <button key={em.code} type="button" onClick={() => toggleEmotion(em.code)} className="flex flex-col items-center gap-1.5">
                                    <span
                                        className="w-11 h-11 rounded-full flex items-center justify-center text-xl transition-all"
                                        style={active ? { backgroundColor: `${em.color}22`, boxShadow: `0 0 0 2px ${em.color}` } : { backgroundColor: "#F3F4F6" }}
                                    >
                                        {em.emoji}
                                    </span>
                                    <span className="text-xs font-medium" style={{ color: active ? em.color : "#9CA3AF" }}>{em.label}</span>
                                </button>
                            );
                        })}
                    </div>
                </div>

                {/* 감정 강도 */}
                <div className="flex flex-col gap-3">
                    <label htmlFor="intensity" className="text-sm font-semibold text-gray-800">감정 강도</label>
                    <div className="flex items-center gap-4">
                        <input
                            id="intensity" type="range" min={1} max={5} step={1}
                            value={emotionIntensity}
                            onChange={(e) => setEmotionIntensity(Number(e.target.value))}
                            className="flex-1 accent-[#4A8EF0]"
                        />
                        <span className="text-sm font-bold text-[#4A8EF0] shrink-0 w-10 text-right">{emotionIntensity} / 5</span>
                    </div>
                </div>

                {errorMessage && <p className="text-red-500 text-sm">{errorMessage}</p>}

                <button
                    type="submit"
                    disabled={isSaving}
                    className="w-full rounded-full bg-[#4A8EF0] hover:bg-[#3a7ee0] text-white font-bold py-4 text-base transition-colors disabled:opacity-60"
                >
                    {isSaving ? "저장 중..." : "저장하기"}
                </button>
            </form>
        </div>
    );
}
