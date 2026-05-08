"use client";
// 파일: app/(app)/report/page.tsx
// 역할: 감정 리포트 페이지 - 주간/월간 감정 통계 (protected)
// 호출: GET /reports/monthly-summary, GET /reports/emotions/weekly → backend-api

import { useState, useEffect } from "react";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
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

const EMOTION_EMOJI: Record<string, string> = {
    happy: "😊", calm: "😌", anxious: "😰",
    sad: "😢", angry: "😠", tired: "😴",
};

interface ChartPoint {
    value: number | null;
    label: string;
    emotion: string | null;
}

function SparklineChart({ data }: { data: ChartPoint[] }) {
    const W = 280;
    const H = 110;
    const padL = 24;
    const padR = 8;
    const padT = 12;
    const padB = 22;
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
            {/* 그리드 라인 + Y 레이블 */}
            {[1, 3, 5].map(v => (
                <g key={v}>
                    <line
                        x1={padL} y1={yOf(v)} x2={W - padR} y2={yOf(v)}
                        stroke="#F3F4F6" strokeWidth="1"
                    />
                    <text x={padL - 4} y={yOf(v) + 3.5} textAnchor="end" fontSize="9" fill="#D1D5DB">
                        {v}
                    </text>
                </g>
            ))}

            {/* 연결선 */}
            {validPts.length > 1 && (
                <polyline
                    points={linePoints}
                    fill="none"
                    stroke="#4A8EF0"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    opacity="0.7"
                />
            )}

            {/* 도트 */}
            {validPts.map((d, i) => (
                <circle
                    key={i}
                    cx={d.x} cy={d.y}
                    r={4}
                    fill={EMOTION_COLOR[d.emotion ?? ""] ?? "#4A8EF0"}
                    stroke="white"
                    strokeWidth="1.5"
                />
            ))}

            {/* X 레이블 */}
            {data.map((d, i) => (
                <text
                    key={i}
                    x={xOf(i)} y={H - 4}
                    textAnchor="middle"
                    fontSize="9"
                    fill="#9CA3AF"
                >
                    {d.label}
                </text>
            ))}
        </svg>
    );
}

function EmotionBars({ distribution }: { distribution: Record<string, number> }) {
    const total = Object.values(distribution).reduce((a, b) => a + b, 0);
    if (total === 0) {
        return <p className="text-sm text-gray-400">기록된 감정이 없습니다.</p>;
    }

    return (
        <div className="flex flex-col gap-3">
            {Object.entries(distribution).map(([emotion, count]) => {
                const percent = Math.round((count / total) * 100);
                return (
                    <div key={emotion} className="flex items-center gap-3">
                          <span className="text-sm text-gray-600 w-16">
                              {EMOTION_EMOJI[emotion] ?? "📔"} {EMOTION_LABEL[emotion] ?? emotion}
                          </span>
                        <div className="flex-1 bg-gray-100 rounded-full h-2.5 overflow-hidden">
                            <div
                                className="h-full rounded-full transition-all duration-500"
                                style={{
                                    width: `${percent}%`,
                                    backgroundColor: EMOTION_COLOR[emotion] ?? "#9CA3AF",
                                }}
                            />
                        </div>
                        <span className="text-sm text-gray-400 w-10 text-right">{percent}%</span>
                    </div>
                );
            })}
        </div>
    );
}
export default function ReportPage() {
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
    const periodLabel = period === "weekly"
        ? (weekly ? `${weekly.startDate} ~ ${weekly.endDate}` : "이번 주")
        : (monthly ? `${monthly.year}년 ${monthly.month}월` : "이번 달");

    return (
        <div className="p-4 md:p-8 max-w-2xl mx-auto w-full">
            <h2 className="text-lg font-semibold text-gray-800 mb-6">감정 리포트</h2>

            {/* 탭 */}
            <div className="flex gap-2 mb-6">
                {(["weekly", "monthly"] as Period[]).map((p) => (
                    <Button
                        key={p}
                        variant={period === p ? "default" : "outline"}
                        className={period === p ? "bg-[#4A8EF0] hover:bg-[#3a7ee0]" : ""}
                        onClick={() => setPeriod(p)}
                    >
                        {p === "weekly" ? "주간" : "월간"}
                    </Button>
                ))}
            </div>

            {isLoading ? (
                <div className="flex items-center justify-center h-48 text-gray-400 text-sm">
                    불러오는 중...
                </div>
            ) : !data ? (
                <div className="flex items-center justify-center h-48 text-gray-400 text-sm">
                    데이터가 없습니다.
                </div>
            ) : (
                <div className="flex flex-col gap-4">

                    {/* 기간 + 요약 통계 */}
                    <Card>
                        <CardContent className="pt-6">
                            <p className="text-sm text-gray-500 mb-3">{periodLabel}</p>
                            <div className="flex gap-6">
                                {data.totalChats > 0 && (
                                    <div>
                                        <p className="text-xs text-gray-400">채팅</p>
                                        <p className="text-xl font-bold text-[#4A8EF0]">{data.totalChats}회</p>
                                    </div>
                                )}
                                <div>
                                    <p className="text-xs text-gray-400">일기</p>
                                    <p className="text-xl font-bold text-[#4A8EF0]">{data.totalDiaries}개</p>
                                </div>
                                {data.averageEmotionScore != null && (
                                    <div>
                                        <p className="text-xs text-gray-400">평균 강도</p>
                                        <p className="text-xl font-bold text-gray-700">
                                            {data.averageEmotionScore.toFixed(1)} / 5
                                        </p>
                                    </div>
                                )}
                                {data.dominantEmotion && (
                                    <div>
                                        <p className="text-xs text-gray-400">주요 감정</p>
                                        <p className="text-xl font-bold text-gray-700">
                                            {EMOTION_EMOJI[data.dominantEmotion] ?? "📔"}{" "}
                                            {EMOTION_LABEL[data.dominantEmotion] ?? data.dominantEmotion}
                                        </p>
                                    </div>
                                )}
                            </div>
                        </CardContent>
                    </Card>

                    {/* 감정 분포 바 */}
                    <Card>
                        <CardHeader className="pb-2">
                            <p className="text-sm font-medium text-gray-700">감정 분포</p>
                        </CardHeader>
                        <CardContent>
                            <EmotionBars distribution={data.emotionDistribution ?? {}} />
                        </CardContent>
                    </Card>
                    {/* 감정 강도 추이 차트 */}
                    {period === "weekly" && weekly && (weekly.dailyTrends?.length ?? 0) > 0 && (
                        <Card>
                            <CardHeader className="pb-2">
                                <p className="text-sm font-medium text-gray-700">감정 강도 추이</p>
                            </CardHeader>
                            <CardContent>
                                <SparklineChart
                                    data={weekly.dailyTrends.map(d => ({
                                        value: d.hasDiary && d.score != null ? d.score : null,
                                        label: new Date(d.date + "T00:00:00").toLocaleDateString("ko-KR", { weekday: "short" }),
                                        emotion: d.emotion,
                                    }))}
                                />
                            </CardContent>
                        </Card>
                    )}
                    {period === "monthly" && monthly && (monthly.weeklySummaries?.length ?? 0) > 0 && (
                        <Card>
                            <CardHeader className="pb-2">
                                <p className="text-sm font-medium text-gray-700">주별 감정 강도 추이</p>
                            </CardHeader>
                            <CardContent>
                                <SparklineChart
                                    data={monthly.weeklySummaries.map(d => ({
                                        value: d.averageScore,
                                        label: `${d.weekNumber}주`,
                                        emotion: d.dominantEmotion,
                                    }))}
                                />
                            </CardContent>
                        </Card>
                    )}
                    {period === "monthly" && monthly?.comparisonWithLastMonth && (() => {
                        const c = monthly.comparisonWithLastMonth!;
                        const trendConfig = {
                            improving: { label: "상승 ↑", color: "text-green-600 bg-green-50" },
                            declining: { label: "하락 ↓", color: "text-red-600 bg-red-50" },
                            stable:    { label: "유지 →", color: "text-gray-600 bg-gray-100" },
                        };
                        const t = trendConfig[c.trend];
                        return (
                            <Card>
                                <CardHeader className="pb-2">
                                    <p className="text-sm font-medium text-gray-700">전월 대비</p>
                                </CardHeader>
                                <CardContent>
                                    <div className="flex gap-6 items-center">
                                        <div>
                                            <p className="text-xs text-gray-400">일기 수</p>
                                            <p className={`text-lg font-bold ${c.diaryCountDiff >= 0 ? "text-green-600" :
                                                "text-red-500"}`}>
                                                {c.diaryCountDiff >= 0 ? "+" : ""}{c.diaryCountDiff}개
                                            </p>
                                        </div>
                                        <div>
                                            <p className="text-xs text-gray-400">감정 강도</p>
                                            <p className={`text-lg font-bold ${c.scoreDiff >= 0 ? "text-green-600" : "text-red-500"}`}>
                                                {c.scoreDiff >= 0 ? "+" : ""}{c.scoreDiff.toFixed(1)}
                                            </p>
                                        </div>
                                        <div>
                                            <p className="text-xs text-gray-400">추세</p>
                                            <span className={`text-sm font-medium px-2 py-0.5 rounded-full ${t.color}`}>
                              {t.label}
                          </span>
                                        </div>
                                    </div>
                                </CardContent>
                            </Card>
                        );
                    })()}
                    {/* AI 분석 */}
                    <Card>
                        <CardHeader className="pb-2">
                            <p className="text-sm font-medium text-gray-700">AI 분석</p>
                        </CardHeader>
                        <CardContent>
                            {aiText ? (
                                <p className="text-sm text-gray-600 bg-gray-50 rounded-lg p-3 leading-relaxed">
                                    {aiText}
                                </p>
                            ) : (
                                <p className="text-sm text-gray-400">분석 결과가 없습니다.</p>
                            )}
                        </CardContent>
                    </Card>

                </div>
            )}
        </div>
    );
}
