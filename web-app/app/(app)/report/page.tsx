"use client";
// 파일: app/(app)/report/page.tsx
// 역할: 감정 리포트 페이지 - 주간/월간 감정 통계 (protected)
// 호출: GET /reports/weekly, GET /reports/monthly → backend-api
// 비고: 디자인 Figma 기준 폴리시. fetch/기간 토글/차트 로직은 보존.
//       Figma의 '위험도 흐름' 카드는 백엔드 위험도 집계 API가 없어 미구현(데이터 없음).

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { ChevronLeft } from "lucide-react";
import { api } from "@/lib/api";

type Period = "weekly" | "monthly";

interface DailyTrend {
    date: string;
    emotion: string | null;
    score: number | null;
    hasDiary: boolean;
}

interface WeeklySummary {
    weekNumber: number;
    diaryCount: number;
    dominantEmotion: string | null;
    averageScore: number | null;
}

interface WeeklyReport {
    startDate: string;
    endDate: string;
    totalDiaries: number;
    totalChats: number;
    averageEmotionScore: number | null;
    emotionDistribution: Record<string, number>;
    dominantEmotion: string | null;
    aiSummary: string | null;
    dailyTrends: DailyTrend[];
}

interface MonthlyReport {
    year: number;
    month: number;
    totalDiaries: number;
    totalChats: number;
    averageEmotionScore: number | null;
    emotionDistribution: Record<string, number>;
    dominantEmotion: string | null;
    aiInsight: string | null;
    weeklySummaries: WeeklySummary[];
    comparisonWithLastMonth: EmotionComparison | null;
}

interface EmotionComparison {
    scoreDiff: number;
    diaryCountDiff: number;
    trend: "improving" | "declining" | "stable";
}

const EMOTION_LABEL: Record<string, string> = {
    happy: "기쁨", calm: "평온", anxious: "불안",
    sad: "슬픔", angry: "화남", tired: "피곤",
};

const EMOTION_COLOR: Record<string, string> = {
    happy: "#FBBF24", calm: "#34D399", anxious: "#F472B6",
    sad: "#60A5FA", angry: "#F87171", tired: "#9CA3AF",
};

interface ChartPoint {
    value: number | null;
    label: string;
    emotion: string | null;
}

function SparklineChart({ data }: { data: ChartPoint[] }) {
    const W = 280;
    const H = 120;
    const padL = 24;
    const padR = 8;
    const padT = 14;
    const padB = 24;
    const innerW = W - padL - padR;
    const innerH = H - padT - padB;

    const n = data.length;
    const xOf = (i: number) => padL + (n <= 1 ? innerW / 2 : (i / (n - 1)) * innerW);
    const yOf = (v: number) => padT + (1 - Math.min(v, 5) / 5) * innerH;

    const validPts = data
        .map((d, i) => ({ ...d, x: xOf(i), y: d.value != null ? yOf(d.value) : null }))
        .filter((d): d is typeof d & { y: number } => d.y != null);

    const linePoints = validPts.map(d => `${d.x.toFixed(1)},${d.y.toFixed(1)}`).join(" ");

    if (validPts.length === 0) {
        return <p className="text-sm text-gray-400 text-center py-4">기록된 데이터가 없습니다.</p>;
    }

    return (
        <svg viewBox={`0 0 ${W} ${H}`} className="w-full">
            {[1, 3, 5].map(v => (
                <g key={v}>
                    <line x1={padL} y1={yOf(v)} x2={W - padR} y2={yOf(v)} stroke="#F3F4F6" strokeWidth="1" />
                    <text x={padL - 4} y={yOf(v) + 3.5} textAnchor="end" fontSize="9" fill="#D1D5DB">{v}</text>
                </g>
            ))}
            {validPts.length > 1 && (
                <polyline
                    points={linePoints}
                    fill="none"
                    stroke="#4A8EF0"
                    strokeWidth="2.5"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                />
            )}
            {validPts.map((d, i) => (
                <circle key={i} cx={d.x} cy={d.y} r={4.5} fill="#4A8EF0" stroke="white" strokeWidth="2" />
            ))}
            {data.map((d, i) => (
                <text key={i} x={xOf(i)} y={H - 5} textAnchor="middle" fontSize="9" fill="#9CA3AF">{d.label}</text>
            ))}
        </svg>
    );
}

// 가장 많이 느낀 감정 — 카운트 알약
function EmotionCountPills({ distribution }: { distribution: Record<string, number> }) {
    const entries = Object.entries(distribution).filter(([, c]) => c > 0).sort((a, b) => b[1] - a[1]);
    if (entries.length === 0) {
        return <p className="text-sm text-gray-400">기록된 감정이 없습니다.</p>;
    }
    return (
        <div className="flex flex-wrap gap-2">
            {entries.map(([emotion, count]) => {
                const color = EMOTION_COLOR[emotion] ?? "#9CA3AF";
                return (
                    <span
                        key={emotion}
                        className="text-sm font-semibold rounded-full px-3.5 py-1.5"
                        style={{ backgroundColor: `${color}22`, color }}
                    >
                        {EMOTION_LABEL[emotion] ?? emotion} {count}회
                    </span>
                );
            })}
        </div>
    );
}

export default function ReportPage() {
    const router = useRouter();
    const [period, setPeriod] = useState<Period>("weekly");
    const [weekly, setWeekly] = useState<WeeklyReport | null>(null);
    const [monthly, setMonthly] = useState<MonthlyReport | null>(null);
    const [isLoading, setIsLoading] = useState(false);

    useEffect(() => {
        async function fetchReport() {
            setIsLoading(true);
            try {
                if (period === "weekly") {
                    const result = await api.get<WeeklyReport>("/api/v1/reports/weekly");
                    setWeekly(result);
                } else {
                    const now = new Date();
                    const result = await api.get<MonthlyReport>(
                        `/api/v1/reports/monthly?year=${now.getFullYear()}&month=${now.getMonth() + 1}`
                    );
                    setMonthly(result);
                }
            } catch {
                // 에러 시 null 유지
            } finally {
                setIsLoading(false);
            }
        }
        fetchReport();
    }, [period]);

    const data = period === "weekly" ? weekly : monthly;
    const aiText = period === "weekly" ? weekly?.aiSummary : monthly?.aiInsight;
    const reportTitle = period === "weekly"
        ? "이번 주 감정 리포트"
        : (monthly ? `${monthly.month}월 감정 리포트` : "이번 달 감정 리포트");
    const reportSubtitle = period === "weekly" ? "이번 주의 감정 흐름과 기록 요약" : "이번 달의 감정 흐름과 기록 요약";

    return (
        <div className="p-4 md:p-8 max-w-2xl mx-auto w-full pb-24">

            {/* 헤더 */}
            <div className="relative flex items-center justify-center mb-5 h-8">
                <button onClick={() => router.back()} className="absolute left-0 text-gray-700 hover:text-gray-900" aria-label="뒤로">
                    <ChevronLeft className="w-6 h-6" />
                </button>
                <h1 className="text-lg font-bold text-gray-800">감정 리포트</h1>
                {/* 기간 토글 */}
                <div className="absolute right-0 flex bg-gray-100 rounded-full p-0.5 text-xs font-semibold">
                    {(["weekly", "monthly"] as Period[]).map((p) => (
                        <button
                            key={p}
                            onClick={() => setPeriod(p)}
                            className={`px-3 py-1 rounded-full transition-colors ${
                                period === p ? "bg-[#4A8EF0] text-white" : "text-gray-500"
                            }`}
                        >
                            {p === "weekly" ? "주간" : "월간"}
                        </button>
                    ))}
                </div>
            </div>

            {isLoading ? (
                <div className="flex items-center justify-center h-48 text-gray-400 text-sm">불러오는 중...</div>
            ) : !data ? (
                <div className="flex items-center justify-center h-48 text-gray-400 text-sm">데이터가 없습니다.</div>
            ) : (
                <div className="flex flex-col gap-4">

                    {/* 요약 카드 */}
                    <div className="rounded-2xl bg-[#EEF3FE] p-5">
                        <h2 className="text-xl font-bold text-gray-800">{reportTitle}</h2>
                        <p className="text-sm text-gray-500 mt-1">{reportSubtitle}</p>
                        <div className="grid grid-cols-3 gap-2 mt-4">
                            <div className="bg-white rounded-xl px-3 py-3 text-center">
                                <p className="text-xs text-gray-400">기록 수</p>
                                <p className="text-xl font-bold text-gray-800 mt-0.5">{data.totalDiaries}</p>
                            </div>
                            <div className="bg-white rounded-xl px-3 py-3 text-center">
                                <p className="text-xs text-gray-400">평균 강도</p>
                                <p className="text-xl font-bold text-gray-800 mt-0.5">
                                    {data.averageEmotionScore != null ? data.averageEmotionScore.toFixed(1) : "-"}
                                </p>
                            </div>
                            <div className="bg-white rounded-xl px-3 py-3 text-center">
                                <p className="text-xs text-gray-400">대표 감정</p>
                                <p className="text-xl font-bold text-gray-800 mt-0.5">
                                    {data.dominantEmotion ? (EMOTION_LABEL[data.dominantEmotion] ?? data.dominantEmotion) : "-"}
                                </p>
                            </div>
                        </div>
                    </div>

                    {/* 감정 변화 추이 */}
                    {period === "weekly" && weekly && (weekly.dailyTrends?.length ?? 0) > 0 && (
                        <div className="rounded-2xl bg-white ring-1 ring-gray-100 shadow-sm p-5">
                            <h3 className="text-base font-bold text-gray-800">감정 변화 추이</h3>
                            <p className="text-xs font-medium text-gray-400 mt-0.5 mb-2">최근 7일 감정 강도 변화</p>
                            <SparklineChart
                                data={weekly.dailyTrends.map(d => ({
                                    value: d.hasDiary && d.score != null ? d.score : null,
                                    label: new Date(d.date + "T00:00:00").toLocaleDateString("ko-KR", { weekday: "short" }),
                                    emotion: d.emotion,
                                }))}
                            />
                        </div>
                    )}
                    {period === "monthly" && monthly && (monthly.weeklySummaries?.length ?? 0) > 0 && (
                        <div className="rounded-2xl bg-white ring-1 ring-gray-100 shadow-sm p-5">
                            <h3 className="text-base font-bold text-gray-800">감정 변화 추이</h3>
                            <p className="text-xs font-medium text-gray-400 mt-0.5 mb-2">주별 감정 강도 변화</p>
                            <SparklineChart
                                data={monthly.weeklySummaries.map(d => ({
                                    value: d.averageScore,
                                    label: `${d.weekNumber}주`,
                                    emotion: d.dominantEmotion,
                                }))}
                            />
                        </div>
                    )}

                    {/* 가장 많이 느낀 감정 */}
                    <div className="rounded-2xl bg-white ring-1 ring-gray-100 shadow-sm p-5">
                        <h3 className="text-base font-bold text-gray-800 mb-3">가장 많이 느낀 감정</h3>
                        <EmotionCountPills distribution={data.emotionDistribution ?? {}} />
                    </div>

                    {/* 전월 대비 (월간) */}
                    {period === "monthly" && monthly?.comparisonWithLastMonth && (() => {
                        const c = monthly.comparisonWithLastMonth!;
                        const trendConfig = {
                            improving: { label: "상승 ↑", color: "text-[#2F855A] bg-[#34D399]/15" },
                            declining: { label: "하락 ↓", color: "text-[#C53030] bg-[#F87171]/15" },
                            stable:    { label: "유지 →", color: "text-gray-600 bg-gray-100" },
                        };
                        const t = trendConfig[c.trend];
                        return (
                            <div className="rounded-2xl bg-white ring-1 ring-gray-100 shadow-sm p-5">
                                <h3 className="text-base font-bold text-gray-800 mb-3">전월 대비</h3>
                                <div className="flex gap-6 items-center">
                                    <div>
                                        <p className="text-xs text-gray-400">일기 수</p>
                                        <p className={`text-lg font-bold ${c.diaryCountDiff >= 0 ? "text-[#2F855A]" : "text-[#C53030]"}`}>
                                            {c.diaryCountDiff >= 0 ? "+" : ""}{c.diaryCountDiff}개
                                        </p>
                                    </div>
                                    <div>
                                        <p className="text-xs text-gray-400">감정 강도</p>
                                        <p className={`text-lg font-bold ${c.scoreDiff >= 0 ? "text-[#2F855A]" : "text-[#C53030]"}`}>
                                            {c.scoreDiff >= 0 ? "+" : ""}{c.scoreDiff.toFixed(1)}
                                        </p>
                                    </div>
                                    <div>
                                        <p className="text-xs text-gray-400">추세</p>
                                        <span className={`text-sm font-semibold px-2.5 py-0.5 rounded-full ${t.color}`}>{t.label}</span>
                                    </div>
                                </div>
                            </div>
                        );
                    })()}

                    {/* AI 분석 — 요약이 있을 때만 표시 */}
                    {aiText && (
                        <div className="rounded-2xl bg-white ring-1 ring-gray-100 shadow-sm p-5">
                            <h3 className="text-base font-bold text-gray-800 mb-2">AI 분석</h3>
                            <p className="text-sm text-gray-600 bg-gray-50 rounded-xl p-4 leading-relaxed">{aiText}</p>
                        </div>
                    )}

                </div>
            )}
        </div>
    );
}
