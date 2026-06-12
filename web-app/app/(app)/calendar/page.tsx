"use client";
// 파일: app/(app)/calendar/page.tsx
// 역할: 월별 감정 캘린더 + 선택 날짜 요약
// 호출: GET /api/v1/calendar?year=&month=  (월 데이터)
//       GET /api/v1/calendar/{date}        (선택 날짜의 일기 목록)
// 비고: 디자인 Figma 기준 폴리시. 감정 표시는 이모지 유지(데모 일관성).
//       날짜 클릭 시 이동 대신 선택 → 하단 요약 카드 인라인 표시.

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { ChevronLeft, ChevronRight, Plus } from "lucide-react";
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

// 선택 날짜 요약용 — GET /api/v1/calendar/{date}
interface DiaryListItem {
    id: number;
    title: string;
    writtenAt: string;
    primaryEmotion: string | null;
    emotionIntensity: number | null;
}

// --- 감정 이모지 매핑 ---

// 실제 모델(tired_v5) + stub predictor 값 모두 커버
const EMOTION_EMOJI: Record<string, string> = {
    happy:   "😊",
    calm:    "😌",
    anxious: "😰",
    sad:     "😢",
    angry:   "😠",
    tired:   "😴",
    joy:      "😊",
    sadness:  "😢",
    anger:    "😠",
    fear:     "😰",
    surprise: "😲",
    disgust:  "🤢",
    neutral:  "😐",
};

// 감정 한국어 라벨 (요약 태그용)
const EMOTION_LABEL: Record<string, string> = {
    happy: "기쁨", joy: "기쁨",
    calm: "평온",
    anxious: "불안", fear: "불안",
    sad: "슬픔", sadness: "슬픔",
    angry: "화남", anger: "화남",
    tired: "지침",
    surprise: "놀람", disgust: "불쾌", neutral: "보통",
};

const DAY_LABELS = ["일", "월", "화", "수", "목", "금", "토"];

function emoji(e: string | null): string {
    return EMOTION_EMOJI[e ?? ""] ?? "📔";
}

// "2024-05-18" → "5월 18일 화요일"
function formatKoreanDate(dateStr: string): string {
    const [y, m, d] = dateStr.split("-").map(Number);
    const dow = DAY_LABELS[new Date(y, m - 1, d).getDay()];
    return `${m}월 ${d}일 ${dow}요일`;
}

// --- 컴포넌트 ---

export default function CalendarPage() {
    const router = useRouter();
    const today = new Date();
    const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, "0")}-${String(today.getDate()).padStart(2, "0")}`;

    const [year, setYear] = useState(today.getFullYear());
    const [month, setMonth] = useState(today.getMonth() + 1); // JS: 0~11 → 백엔드: 1~12
    const [data, setData] = useState<CalendarMonthData | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    const [selectedDate, setSelectedDate] = useState<string>(todayStr);
    const [dayDiaries, setDayDiaries] = useState<DiaryListItem[]>([]);
    const [isDayLoading, setIsDayLoading] = useState(false);

    // 월 데이터 조회
    useEffect(() => {
        async function fetchCalendar() {
            setIsLoading(true);
            try {
                const result = await api.get<CalendarMonthData>(
                    `/api/v1/calendar?year=${year}&month=${month}`
                );
                setData(result);
            } catch {
                // 토큰 만료 등은 api 레이어가 처리
            } finally {
                setIsLoading(false);
            }
        }
        fetchCalendar();
    }, [year, month]);

    // 선택 날짜의 일기 목록 조회
    useEffect(() => {
        async function fetchDay() {
            setIsDayLoading(true);
            try {
                const result = await api.get<DiaryListItem[]>(`/api/v1/calendar/${selectedDate}`);
                setDayDiaries(result ?? []);
            } catch {
                setDayDiaries([]);
            } finally {
                setIsDayLoading(false);
            }
        }
        fetchDay();
    }, [selectedDate]);

    function goPrev() {
        if (month === 1) { setYear(y => y - 1); setMonth(12); }
        else { setMonth(m => m - 1); }
    }
    function goNext() {
        if (month === 12) { setYear(y => y + 1); setMonth(1); }
        else { setMonth(m => m + 1); }
    }

    // --- 캘린더 그리드 계산 ---
    const firstDayOfWeek = new Date(year, month - 1, 1).getDay();
    const daysInMonth    = new Date(year, month, 0).getDate();

    const dayMap: Record<string, CalendarDay> = {};
    data?.days.forEach(d => { dayMap[d.date] = d; });

    const cells: (number | null)[] = [
        ...Array(firstDayOfWeek).fill(null),
        ...Array.from({ length: daysInMonth }, (_, i) => i + 1),
    ];

    return (
        <div className="p-4 md:p-8 max-w-2xl mx-auto w-full pb-28">

            {/* 브랜드 헤더 */}
            <div className="flex items-center justify-between mb-3">
                <span className="text-base font-bold text-[#4A8EF0]">감정나침반</span>
                <span className="w-8 h-8 rounded-full bg-gray-200" aria-hidden />
            </div>

            {/* 월 네비 + 기록일 배지 */}
            <div className="flex items-center justify-between mb-5">
                <div className="flex items-center gap-1">
                    <button onClick={goPrev} aria-label="이전 달" className="text-gray-500 hover:text-gray-800 p-1">
                        <ChevronLeft className="w-5 h-5" />
                    </button>
                    <h2 className="text-2xl font-bold text-gray-800">{year}년 {month}월</h2>
                    <button onClick={goNext} aria-label="다음 달" className="text-gray-500 hover:text-gray-800 p-1">
                        <ChevronRight className="w-5 h-5" />
                    </button>
                </div>
                {data && (
                    <span className="flex items-center gap-1.5 bg-[#FBBF24]/15 text-[#B7791F] text-xs font-semibold rounded-full px-3 py-1.5">
                        <span className="w-1.5 h-1.5 rounded-full bg-[#FBBF24]" />
                        기록일 {data.totalDiaries}일
                    </span>
                )}
            </div>

            {/* 요일 헤더 */}
            <div className="grid grid-cols-7 mb-1">
                {DAY_LABELS.map(label => (
                    <div key={label} className="text-center text-xs font-medium text-gray-400 py-1">
                        {label}
                    </div>
                ))}
            </div>

            {/* 날짜 그리드 */}
            {isLoading ? (
                <div className="h-64 flex items-center justify-center text-gray-400 text-sm">불러오는 중...</div>
            ) : (
                <div className="grid grid-cols-7 gap-y-1">
                    {cells.map((day, idx) => {
                        if (day === null) return <div key={`empty-${idx}`} />;

                        const dateStr = `${year}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
                        const dayData = dayMap[dateStr];
                        const isToday = dateStr === todayStr;
                        const isSelected = dateStr === selectedDate;

                        return (
                            <button
                                key={dateStr}
                                onClick={() => setSelectedDate(dateStr)}
                                className={`aspect-square flex flex-col items-center justify-start pt-1.5 rounded-xl text-sm transition-colors cursor-pointer
                                    ${isSelected ? "ring-2 ring-[#4A8EF0] bg-[#4A8EF0]/5" : "hover:bg-gray-50"}`}
                            >
                                <span className={`text-xs font-semibold ${isToday ? "text-[#4A8EF0]" : "text-gray-700"}`}>
                                    {day}
                                </span>
                                {dayData?.hasDiary && (dayData.diaries?.length ?? 0) > 0 && (
                                    <div className="flex gap-0.5 mt-0.5">
                                        {(dayData.diaries ?? []).slice(0, 3).map((d, i) => (
                                            <span key={i} className="text-xs leading-none">{emoji(d.primaryEmotion)}</span>
                                        ))}
                                    </div>
                                )}
                            </button>
                        );
                    })}
                </div>
            )}

            {/* 선택 날짜 요약 카드 */}
            <div className="mt-6 rounded-2xl bg-white ring-1 ring-gray-100 shadow-sm p-5">
                <h3 className="text-lg font-bold text-gray-800">{formatKoreanDate(selectedDate)}</h3>
                <p className="text-xs font-medium text-gray-400 mt-0.5">선택 날짜 요약</p>

                {isDayLoading ? (
                    <p className="text-sm text-gray-400 mt-3">불러오는 중...</p>
                ) : dayDiaries.length === 0 ? (
                    <p className="text-sm text-gray-500 mt-3">이 날의 기록이 없어요.</p>
                ) : (
                    <div className="mt-3">
                        <p className="text-sm text-gray-700">오늘 기록이 {dayDiaries.length}개 있어요.</p>
                        <div className="flex flex-col gap-2 mt-3">
                            {dayDiaries.map((d) => (
                                <button
                                    key={d.id}
                                    onClick={() => router.push(`/diary/${d.id}`)}
                                    className="flex items-center gap-2 text-left rounded-xl bg-gray-50 hover:bg-gray-100 px-3 py-2.5 transition-colors"
                                >
                                    <span className="text-lg">{emoji(d.primaryEmotion)}</span>
                                    <span className="flex-1 text-sm text-gray-700 truncate">{d.title}</span>
                                    {d.primaryEmotion && EMOTION_LABEL[d.primaryEmotion] && (
                                        <span className="text-xs text-gray-400 shrink-0">{EMOTION_LABEL[d.primaryEmotion]}</span>
                                    )}
                                </button>
                            ))}
                        </div>
                    </div>
                )}
            </div>

            {/* 오늘 기록 남기기 CTA 카드 */}
            <div className="mt-4 rounded-2xl bg-white ring-1 ring-gray-100 shadow-sm p-5">
                <h3 className="text-lg font-bold text-gray-800">오늘 기록을 남겨보세요</h3>
                <p className="text-sm text-gray-400 mt-1">선택한 날짜의 감정과 생각을 짧게 적어보세요.</p>
                <button
                    onClick={() => router.push("/diary/new")}
                    className="mt-4 rounded-full bg-[#4A8EF0] hover:bg-[#3a7ee0] text-white font-bold px-6 py-3 transition-colors"
                >
                    일기 작성하기
                </button>
            </div>

            {/* 플로팅 작성 버튼 */}
            <button
                onClick={() => router.push("/diary/new")}
                aria-label="일기 작성"
                className="fixed bottom-20 right-5 md:bottom-8 w-14 h-14 rounded-full bg-[#4A8EF0] hover:bg-[#3a7ee0] text-white shadow-lg flex items-center justify-center z-40 transition-colors"
            >
                <Plus className="w-7 h-7" />
            </button>
        </div>
    );
}
