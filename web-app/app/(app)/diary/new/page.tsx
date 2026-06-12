"use client";
export const dynamic = "force-dynamic";
// 파일: app/(app)/diary/new/page.tsx
// 역할: 일기 작성 페이지 ("오늘의 기록")
// 호출: POST /api/v1/diaries → backend-api
// 비고: 디자인 Figma 기준 폴리시. 제출/3개 제한 로직은 무수정.
//       날짜/시간은 Figma에 입력칸이 없어 숨기고 기본값(오늘/현재시각) 사용.
//       감정 태그는 백엔드 미지원이라 미구현.

import { useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { ChevronLeft } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { api } from "@/lib/api";

// 실제 모델 레이블 기준 (kcelectra_predictor.py) + 선택 링 색상
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

// "2024-05-15" + "09:30" → "2024-05-15T09:30:00"
function toLocalDateTime(date: string, time: string): string {
    return `${date}T${time}:00`;
}

export default function DiaryNewPage() {
    const router = useRouter();
    const searchParams = useSearchParams();
    const today = new Date();

    const [title, setTitle] = useState("");
    const [content, setContent] = useState("");
    // 날짜/시간은 Figma에 UI가 없어 기본값으로 고정(달력에서 ?date= 전달 시 그 날짜)
    const [date] = useState(searchParams.get("date") ?? today.toISOString().slice(0, 10));
    const [time] = useState(today.toTimeString().slice(0, 5));
    const [selectedEmotion, setSelectedEmotion] = useState<string | null>(null);
    const [emotionIntensity, setEmotionIntensity] = useState(3);
    const [isLoading, setIsLoading] = useState(false);
    const [errorMessage, setErrorMessage] = useState("");

    async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
        e.preventDefault();
        setErrorMessage("");

        if (!title.trim()) { setErrorMessage("제목을 입력해주세요."); return; }
        if (!content.trim()) { setErrorMessage("내용을 입력해주세요."); return; }

        setIsLoading(true);
        try {
            await api.post(
                "/api/v1/diaries",
                {
                    title,
                    content,
                    writtenAt: toLocalDateTime(date, time),
                    ...(selectedEmotion && { primaryEmotion: selectedEmotion }),
                    ...(emotionIntensity && { emotionIntensity }),
                }
            );
            router.push(`/diary?date=${date}`);
        } catch (e: unknown) {
            const msg = e instanceof Error ? e.message : "";
            if (msg.includes("D003") || msg.includes("3개")) {
                setErrorMessage("이 날은 이미 3개의 일기를 작성했어요. 내일 또 기록해보세요 😊");
            } else {
                setErrorMessage("일기 저장에 실패했습니다. 다시 시도해주세요.");
            }
        } finally {
            setIsLoading(false);
        }
    }

    // 같은 감정을 다시 누르면 해제 (토글)
    function toggleEmotion(code: string) {
        setSelectedEmotion(prev => prev === code ? null : code);
    }

    return (
        <div className="p-4 md:p-8 max-w-2xl mx-auto w-full pb-28">

            {/* 헤더 — chevron 좌측, 제목 가운데 */}
            <div className="relative flex items-center justify-center mb-5 h-8">
                <button onClick={() => router.back()} className="absolute left-0 text-gray-700 hover:text-gray-900" aria-label="뒤로">
                    <ChevronLeft className="w-6 h-6" />
                </button>
                <h1 className="text-lg font-bold text-gray-800">오늘의 기록</h1>
            </div>

            {/* 인트로 카드 */}
            <div className="rounded-2xl bg-[#EEF3FE] p-5 mb-6">
                <h2 className="text-lg font-bold text-gray-800">하루를 기록해보세요</h2>
                <p className="text-sm text-gray-500 leading-relaxed mt-1">
                    오늘 느낀 감정과 기억에 남는 순간을 적으면<br />
                    AI가 감정 흐름을 함께 정리해드려요.
                </p>
            </div>

            <form onSubmit={handleSubmit} className="flex flex-col gap-6">

                {/* 제목 */}
                <div className="flex flex-col gap-2">
                    <label htmlFor="title" className="text-sm font-semibold text-gray-800">제목</label>
                    <Input
                        id="title"
                        placeholder="오늘 하루를 한 문장으로 적어보세요"
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
                            placeholder="오늘 있었던 일을 자유롭게 기록해보세요."
                            value={content}
                            onChange={(e) => setContent(e.target.value)}
                            className={`min-h-[200px] resize-none pb-7 ${FIELD_CLASS} py-4`}
                            maxLength={10000}
                            required
                        />
                        <span className="absolute bottom-3 right-4 text-xs text-gray-400">
                            {content.length} / 10000
                        </span>
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
                                <button
                                    key={em.code}
                                    type="button"
                                    onClick={() => toggleEmotion(em.code)}
                                    className="flex flex-col items-center gap-1.5"
                                >
                                    <span
                                        className="w-11 h-11 rounded-full flex items-center justify-center text-xl transition-all"
                                        style={
                                            active
                                                ? { backgroundColor: `${em.color}22`, boxShadow: `0 0 0 2px ${em.color}` }
                                                : { backgroundColor: "#F3F4F6" }
                                        }
                                    >
                                        {em.emoji}
                                    </span>
                                    <span
                                        className="text-xs font-medium"
                                        style={{ color: active ? em.color : "#9CA3AF" }}
                                    >
                                        {em.label}
                                    </span>
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
                            id="intensity"
                            type="range"
                            min={1}
                            max={5}
                            step={1}
                            value={emotionIntensity}
                            onChange={(e) => setEmotionIntensity(Number(e.target.value))}
                            className="flex-1 accent-[#4A8EF0]"
                        />
                        <span className="text-sm font-bold text-[#4A8EF0] shrink-0 w-10 text-right">
                            {emotionIntensity} / 5
                        </span>
                    </div>
                </div>

                {errorMessage && (
                    <p className="text-red-500 text-sm">{errorMessage}</p>
                )}

                {/* 저장 */}
                <button
                    type="submit"
                    disabled={isLoading}
                    className="w-full rounded-full bg-[#4A8EF0] hover:bg-[#3a7ee0] text-white font-bold py-4 text-base transition-colors disabled:opacity-60"
                >
                    {isLoading ? "저장 중..." : "저장하기"}
                </button>
            </form>
        </div>
    );
}
