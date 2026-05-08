"use client";
// 파일: app/(app)/diary/new/page.tsx
// 역할: 일기 작성 페이지
// 호출: POST /api/v1/diaries → backend-api

import { useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { api } from "@/lib/api";

// 실제 모델 레이블 기준 (kcelectra_predictor.py)
const EMOTIONS = [
  { code: "happy",   emoji: "😊", label: "기쁨" },
  { code: "calm",    emoji: "😌", label: "평온" },
  { code: "anxious", emoji: "😰", label: "불안" },
  { code: "sad",     emoji: "😢", label: "슬픔" },
  { code: "angry",   emoji: "😠", label: "화남" },
  { code: "tired",   emoji: "😴", label: "피곤" },
];

const INTENSITIES = [1, 2, 3, 4, 5];

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
  const [date, setDate] = useState(searchParams.get("date") ?? today.toISOString().slice(0, 10));
  const [time, setTime] = useState(today.toTimeString().slice(0, 5));   // "09:30"
  const [selectedEmotion, setSelectedEmotion] = useState<string | null>(null);
  const [emotionIntensity, setEmotionIntensity] = useState<number | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setIsLoading(true);
    setErrorMessage("");

    // 공백만 입력한 경우 차단
    if (!title.trim()) {
      setErrorMessage("제목을 입력해주세요.");
      return;
    }
    if (!content.trim()) {
      setErrorMessage("내용을 입력해주세요.");
      return;
    }

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
  // 같은 감정을 다시 누르면 해제 (토글)                                                                    [418/1961]
  function toggleEmotion(code: string) {
    setSelectedEmotion(prev => prev === code ? null : code);
    setEmotionIntensity(null); // 감정 바꾸면 강도 초기화
  }

  return (
      <div className="p-4 md:p-8 max-w-2xl mx-auto w-full">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-lg font-semibold text-gray-800">일기 쓰기</h2>
          <span className="text-xs text-gray-400">하루 최대 3개</span>
        </div>

        <form onSubmit={handleSubmit} className="flex flex-col gap-6">

          {/* 기본 정보 */}
          <Card>
            <CardContent className="pt-6 flex flex-col gap-4">

              <div className="flex flex-col gap-1.5">
                <Label htmlFor="title">제목</Label>
                <Input
                    id="title"
                    placeholder="오늘 하루를 한 줄로 표현하면?"
                    value={title}
                    onChange={(e) => setTitle(e.target.value)}
                    maxLength={200}
                    required
                />
              </div>

              {/* 날짜 + 시간 나란히 */}
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
                    placeholder="오늘 있었던 일을 자유롭게 기록해보세요."
                    value={content}
                    onChange={(e) => setContent(e.target.value)}
                    className="min-h-[200px] resize-none"
                    maxLength={10000}
                    required
                />
                {/* 글자수 카운터 */}
                <p className="text-xs text-gray-400 text-right">
                  {content.length} / 10,000
                </p>
              </div>

            </CardContent>
          </Card>
          {/* 감정 선택 */}                                                                                   [346/1961]
          <Card>
            <CardHeader className="pb-2">
              <p className="text-sm font-medium text-gray-700">
                오늘의 감정{" "}
                <span className="text-gray-400 font-normal">(선택)</span>
              </p>
            </CardHeader>
            <CardContent className="flex flex-col gap-4">

              <div className="grid grid-cols-6 gap-2">
                {EMOTIONS.map((em) => (
                    <button
                        key={em.code}
                        type="button"  // ← form 안에서 submit 방지!
                        onClick={() => toggleEmotion(em.code)}
                        className={`flex flex-col items-center gap-1 py-2 rounded-lg border
                      transition-colors
                      ${selectedEmotion === em.code
                            ? "border-[#4A8EF0] bg-blue-50"
                            : "border-gray-200 hover:bg-gray-50"
                        }`}
                    >
                      <span className="text-xl">{em.emoji}</span>
                      <span className="text-xs text-gray-600">{em.label}</span>
                    </button>
                ))}
              </div>

              {/* 감정 강도: 감정 선택했을 때만 표시 */}
              {selectedEmotion && (
                  <div className="flex flex-col gap-2">
                    <p className="text-sm text-gray-600">감정 강도</p>
                    <div className="flex gap-2">
                      {INTENSITIES.map((n) => (
                          <button
                              key={n}
                              type="button"
                              onClick={() =>
                                  setEmotionIntensity(prev => prev === n ? null : n)
                              }
                              className={`flex-1 py-2 rounded-lg border text-sm font-medium
                          transition-colors
                          ${emotionIntensity === n
                                  ? "border-[#4A8EF0] bg-blue-50 text-[#4A8EF0]"
                                  : "border-gray-200 text-gray-600 hover:bg-gray-50"
                              }`}
                          >
                            {n}
                          </button>
                      ))}
                    </div>
                    <div className="flex justify-between text-xs text-gray-400 px-1">
                      <span>약함</span>
                      <span>강함</span>
                    </div>
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
                disabled={isLoading}
                className="flex-1 bg-[#4A8EF0] hover:bg-[#3a7ee0]"
            >
              {isLoading ? "저장 중..." : "저장하기"}
            </Button>
          </div>

        </form>
      </div>
  );
}
