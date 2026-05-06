"use client";                                                                                               [345/1969]
// 파일: app/(app)/diary/[id]/page.tsx
// 역할: 일기 상세 조회 페이지
// 호출: GET /api/v1/diaries/{id} → backend-api

import { useState, useEffect } from "react";
import { useRouter, useParams } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { ChevronLeft, Pencil, Trash2 } from "lucide-react";
import { api } from "@/lib/api";

// --- 타입 정의 ---

interface AiAnalysis {
  primaryEmotion: string | null;
  emotionIntensity: number | null;
  summary: string | null;
  confidence: number | null;
  riskLevel: string | null;
  recommendedAction: string | null;
}

interface DiaryDetail {
  id: number;
  title: string;
  content: string;
  writtenAt: string;
  primaryEmotion: string | null;
  emotionIntensity: number | null;
  aiAnalysis: AiAnalysis | null;
  createdAt: string;
  updatedAt: string;
}

interface ApiResponse<T> {
  success: boolean;
  data: T;
}
// --- 상수 ---                                                                                             [305/1969]

const EMOTION_EMOJI: Record<string, string> = {
  happy: "😊", calm: "😌", anxious: "😰",
  sad: "😢", angry: "😠", tired: "😴",
};

const EMOTION_LABEL: Record<string, string> = {
  happy: "기쁨", calm: "평온", anxious: "불안",
  sad: "슬픔", angry: "화남", tired: "피곤",
};

const RISK_STYLE: Record<string, string> = {
  LOW:    "text-green-600 bg-green-50",
  MEDIUM: "text-yellow-600 bg-yellow-50",
  HIGH:   "text-red-600 bg-red-50",
};

const RISK_LABEL: Record<string, string> = {
  LOW: "낮음", MEDIUM: "보통", HIGH: "높음",
};

// --- 날짜 포맷 ---

function formatDateTime(dateStr: string): string {
  return new Date(dateStr).toLocaleString("ko-KR", {
    year: "numeric", month: "long", day: "numeric",
    hour: "2-digit", minute: "2-digit",
  });
}

// --- 컴포넌트 ---

export default function DiaryDetailPage() {
  const router = useRouter();
  const params = useParams();
  const id = params.id as string;  // URL의 [id] 부분

  const [diary, setDiary] = useState<DiaryDetail | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isDeleting, setIsDeleting] = useState(false);

  useEffect(() => {
    async function fetchDiary() {
      try {
        const result = await api.get<ApiResponse<DiaryDetail>>(
            `/api/v1/diaries/${id}`
        );
        setDiary(result.data);
      } catch {
        router.push("/calendar"); // 없는 일기면 캘린더로
      } finally {
        setIsLoading(false);
      }
    }
    fetchDiary();
  }, [id]);

  async function handleDelete() {
    if (!window.confirm("일기를 삭제할까요?")) return;

    setIsDeleting(true);
    try {
      await api.delete(`/api/v1/diaries/${id}`);
      router.push("/calendar");
    } catch {
      setIsDeleting(false);
    }
  }

  // 로딩 중
  if (isLoading) {
    return (
        <div className="flex-1 flex items-center justify-center text-gray-400 text-sm">
          불러오는 중...
        </div>
    );
  }

  // 데이터 없음 (에러 시 이미 redirect됨)
  if (!diary) return null;
  return (
      <div className="p-4 md:p-8 max-w-2xl mx-auto w-full">

        {/* 헤더: 뒤로가기 + 수정/삭제 */}
        <div className="flex items-center justify-between mb-6">
          <Button variant="ghost" size="icon" onClick={() => router.back()}>
            <ChevronLeft className="w-5 h-5" />
          </Button>
          <div className="flex gap-1">
            <Button
                variant="ghost"
                size="icon"
                onClick={() => router.push(`/diary/${id}/edit`)}
            >
              <Pencil className="w-4 h-4" />
            </Button>
            <Button
                variant="ghost"
                size="icon"
                onClick={handleDelete}
                disabled={isDeleting}
                className="text-red-400 hover:text-red-600 hover:bg-red-50"
            >
              <Trash2 className="w-4 h-4" />
            </Button>
          </div>
        </div>

        {/* 날짜 */}
        <p className="text-sm text-gray-400 mb-2">
          {formatDateTime(diary.writtenAt)}
        </p>

        {/* 제목 */}
        <h1 className="text-xl font-bold text-gray-800 mb-4">
          {diary.title}
        </h1>

        {/* 감정 배지 */}
        {diary.primaryEmotion && (
            <div className="flex items-center gap-2 mb-6">
            <span className="text-2xl">
              {EMOTION_EMOJI[diary.primaryEmotion] ?? "📔"}
            </span>
              <span className="text-sm text-gray-600">
              {EMOTION_LABEL[diary.primaryEmotion] ?? diary.primaryEmotion}
                {diary.emotionIntensity && (
                    <span className="text-gray-400 ml-1">
                  강도 {diary.emotionIntensity}/5
                </span>
                )}
            </span>
            </div>
        )}

        {/* 일기 내용 */}
        <Card className="mb-4">
          <CardContent className="pt-6">
            {/* whitespace-pre-wrap: 줄바꿈(\n)을 화면에서 보존 */}
            <p className="text-gray-700 whitespace-pre-wrap leading-relaxed">
              {diary.content}
            </p>
          </CardContent>
        </Card>
        [159/1969]
        {/* AI 분석 결과 */}
        <Card>
          <CardHeader className="pb-2">
            <p className="text-sm font-medium text-gray-700">AI 분석</p>
          </CardHeader>
          <CardContent>
            {diary.aiAnalysis ? (
                <div className="flex flex-col gap-3">

                  {diary.aiAnalysis.summary && (
                      <p className="text-sm text-gray-600 bg-gray-50 rounded-lg p-3">
                        {diary.aiAnalysis.summary}
                      </p>
                  )}

                  {diary.aiAnalysis.riskLevel && (
                      <div className="flex items-center gap-2">
                        <span className="text-xs text-gray-500">위험도</span>
                        <span className={`text-xs font-medium px-2 py-0.5 rounded-full
                      ${RISK_STYLE[diary.aiAnalysis.riskLevel]
                        ?? "text-gray-600 bg-gray-100"}`}>
                      {RISK_LABEL[diary.aiAnalysis.riskLevel]
                          ?? diary.aiAnalysis.riskLevel}
                    </span>
                      </div>
                  )}

                  {diary.aiAnalysis.recommendedAction && (
                      <p className="text-xs text-[#4A8EF0]">
                        💡 {diary.aiAnalysis.recommendedAction}
                      </p>
                  )}

                </div>
            ) : (
                // aiAnalysis가 null이면 → 아직 분석 전 or AI 실패
                <p className="text-sm text-gray-400">
                  분석 중이거나 아직 분석 결과가 없습니다.
                </p>
            )}
          </CardContent>
        </Card>

      </div>
  );
}