"use client";
// 파일: app/(app)/calendar/page.tsx
// 역할: 월별 감정 캘린더 화면
// 호출: GET /api/v1/calendar?year=&month= → backend-api

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { api } from "@/lib/api";

// --- 타입 정의 ---

interface DiaryBrief {
    id: number;
    primaryEmotion: string | null;
}

interface CalendarDay {
    date: string;               // "2024-05-15"
    diaries: DiaryBrief[];      // 하루 최대 3개
    hasDiary: boolean;
}

interface CalendarMonthData {
    year: number;
    month: number;
    days: CalendarDay[];
    emotionSummary: Record<string, number>;
    totalDiaries: number;
}

// --- 감정 이모지 매핑 ---

// 실제 모델(tired_v5) + stub predictor 값 모두 커버
const EMOTION_EMOJI: Record<string, string> = {
    // 실제 모델 레이블 (소문자 6가지)
    happy:   "😊",
    calm:    "😌",
    anxious: "😰",
    sad:     "😢",
    angry:   "😠",
    tired:   "😴",
    // stub predictor 레이블 (개발환경)
    joy:      "😊",
    sadness:  "😢",
    anger:    "😠",
    fear:     "😰",
    surprise: "😲",
    disgust:  "🤢",
    neutral:  "😐",
};

const DAY_LABELS = ["일", "월", "화", "수", "목", "금", "토"];

// --- 컴포넌트 ---

export default function CalendarPage() {
    const router = useRouter();
    const today = new Date();

    const [year, setYear] = useState(today.getFullYear());
    const [month, setMonth] = useState(today.getMonth() + 1); // JS: 0~11 → 백엔드: 1~12
    const [data, setData] = useState<CalendarMonthData | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    // year나 month가 바뀔 때마다 자동 호출
    useEffect(() => {
        async function fetchCalendar() {
            setIsLoading(true);
            try {
                const result = await api.get<CalendarMonthData>(
                    `/api/v1/calendar?year=${year}&month=${month}`
                );
                setData(result);
            } catch {
                // 토큰 만료 등 에러는 추후 처리
            } finally {
                setIsLoading(false);
            }
        }
        fetchCalendar();
    }, [year, month]);
// 이전 달
    function goPrev() {
        if (month === 1) { setYear(y => y - 1); setMonth(12); }
        else { setMonth(m => m - 1); }
    }

    // 다음 달
    function goNext() {
        if (month === 12) { setYear(y => y + 1); setMonth(1); }
        else { setMonth(m => m + 1); }
    }

    // 날짜 클릭 → 날짜별 일기 목록 페이지로 이동
    function handleDayClick(dateStr: string) {
        router.push(`/diary?date=${dateStr}`);
    }

    // --- 캘린더 그리드 계산 ---

    const firstDayOfWeek = new Date(year, month - 1, 1).getDay(); // 1일이 무슨 요일?
    const daysInMonth    = new Date(year, month, 0).getDate();     // 이 달 총 일수

    // 빠른 조회를 위해 date 문자열 → CalendarDay 맵으로 변환
    const dayMap: Record<string, CalendarDay> = {};
    data?.days.forEach(d => { dayMap[d.date] = d; });

    // 빈 칸(null) + 날짜(1~31) 혼합 배열
    const cells: (number | null)[] = [
        ...Array(firstDayOfWeek).fill(null),
        ...Array.from({ length: daysInMonth }, (_, i) => i + 1),
    ];

    return (
        <div className="p-4 md:p-8 max-w-2xl mx-auto w-full">

            {/* 헤더 */}
            <div className="flex items-center justify-between mb-6">
                <Button variant="ghost" size="icon" onClick={goPrev}>
                    <ChevronLeft className="w-5 h-5" />
                </Button>
                <h2 className="text-lg font-semibold text-gray-800">
                    {year}년 {month}월
                </h2>
                <Button variant="ghost" size="icon" onClick={goNext}>
                    <ChevronRight className="w-5 h-5" />
                </Button>
            </div>

            <Card>
                <CardContent className="pt-4">

                    {/* 요일 헤더 */}
                    <div className="grid grid-cols-7 mb-2">
                        {DAY_LABELS.map(label => (
                            <div key={label}
                                 className="text-center text-xs font-medium text-gray-400 py-1">
                                {label}
                            </div>
                        ))}
                    </div>
                    {/* 날짜 그리드 */}                                                                                [52/1990]
                    {isLoading ? (
                        <div className="h-64 flex items-center justify-center text-gray-400 text-sm">
                            불러오는 중...
                        </div>
                    ) : (
                        <div className="grid grid-cols-7 gap-1">
                            {cells.map((day, idx) => {
                                if (day === null) {
                                    return <div key={`empty-${idx}`} />;
                                }

                                // "2024-05-03" 형식으로 만들기
                                const dateStr = `${year}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
                                const dayData = dayMap[dateStr];
                                const isToday =
                                    day === today.getDate() &&
                                    month === today.getMonth() + 1 &&
                                    year === today.getFullYear();

                                return (
                                    <button
                                        key={dateStr}
                                        onClick={() => handleDayClick(dateStr)}
                                        className={`
                        aspect-square flex flex-col items-center justify-center
                        rounded-lg text-sm transition-colors
                        ${isToday ? "ring-2 ring-[#4A8EF0]" : ""}
                        ${dayData?.hasDiary
                                            ? "hover:bg-blue-50 cursor-pointer"
                                            : "hover:bg-gray-50 cursor-pointer"}
                      `}
                                    >
                      <span className={`text-xs font-medium
                        ${isToday ? "text-[#4A8EF0]" : "text-gray-700"}`}>
                        {day}
                      </span>
                                        {dayData?.hasDiary && (dayData.diaries?.length ?? 0) > 0 && (
                                            <div className="flex gap-0.5 mt-0.5">
                                                {(dayData.diaries ?? []).slice(0, 3).map((d, i) => (
                                                    <span key={i} className="text-xs leading-none">
                                                        {EMOTION_EMOJI[d.primaryEmotion ?? ""] ?? "📔"}
                                                    </span>
                                                ))}
                                            </div>
                                        )}
                                    </button>
                                );
                            })}
                        </div>
                    )}

                </CardContent>
            </Card>
            {/* 이번 달 요약 */}
            {data && (
                <p className="mt-4 text-sm text-gray-500 text-center">
                    이번 달 기록{" "}
                    <span className="font-semibold text-[#4A8EF0]">
              {data.totalDiaries}개
            </span>
                </p>
            )}

        </div>
    );
}
