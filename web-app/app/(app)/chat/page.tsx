"use client";
// 파일: app/(app)/chat/page.tsx
// 역할: AI 채팅 페이지 - 상담형 대화 (protected)
// 호출: POST /chat/sessions, POST /chat/sessions/{id}/messages → backend-api → ai-api

import { useState, useEffect } from "react";
import { api } from "@/src/lib/api";

//1단계: 타입 정의
// app/(app)/chat/page.tsx 상단에
type Message = {
  id: number;
  role: "user" | "assistant";
  content: string;
  responseType?: "NORMAL" | "SAFETY" | "SUPPORTIVE" | "FALLBACK";
};

export default function ChatPage() {
  //2단계: 상태 정의
  const [sessionId, setSessionId] = useState<number | null>(null);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [messages, setMessages] = useState<Message[]>([
    {
      id: 0,
      role: "assistant",
      content: "안녕하세요. 오늘 어떠셨나요? 편하게 이야기해주세요.",
    },
  ]);

//3단계: 세션 생성 (페이지 첫 진입 시)
  // ✅ 실제 응답 구조
  // { success: true, data: { id: 1, title: "새 상담", status: "ACTIVE", ... } }
  useEffect(() => {
    api.post<{ id: number }>("/api/v1/chat/sessions", {
      title: "새 상담",
    }).then((res) => setSessionId(res.id));
  }, []);

//4단계: 메시지 전송                                                                                            [39/640]
  async function handleSend() {
    if (!input.trim() || !sessionId || loading) return;

    const userMsg: Message = { id: Date.now(), role: "user", content: input };
    setMessages((prev) => [...prev, userMsg]);
    setInput("");
    setLoading(true);

    try {
      const res = await api.post<{
        id: number;
        role: string;
        content: string;
      }>(`/api/v1/chat/sessions/${sessionId}/messages`, { content: input });

      setMessages((prev) => [
        ...prev,
        {
          id: res.id,           // assistantMessageId → id
          role: "assistant",
          content: res.content, // assistantReply → content
          // responseType은 백엔드가 안 내려줌 → 제거
        },
      ]);
    } finally {
      setLoading(false);
    }
  }

//5단계: SAFETY 분기 UI 처리
// 메시지 렌더링 시 responseType에 따라 색상 구분
// SAFETY → 빨간 테두리, FALLBACK → 회색, NORMAL → 기본
  const getBubbleStyle = (msg: Message) => {
    if (msg.responseType === "SAFETY")
      return "bg-red-50 border border-red-200 text-red-700";
    if (msg.responseType === "FALLBACK")
      return "bg-gray-50 border border-gray-200 text-gray-500";
    return "bg-white border border-gray-100 text-gray-700";
  };


  return (
      <div className="flex flex-col h-full max-w-2xl mx-auto w-full">
        <header className="px-4 md:px-8 py-4 bg-white border-b border-gray-100">
          <h1 className="text-xl font-bold text-gray-800">AI 대화</h1>
          <p className="text-xs text-gray-400 mt-0.5">마음을 편하게 이야기해보세요</p>
        </header>

        {/* 실제 메시지 목록 */}
        <div className="flex-1 overflow-y-auto p-4 md:p-8 flex flex-col gap-4">
          {messages.map((msg) => (
              <div key={msg.id}
                   className={`flex gap-3 items-start ${msg.role === "user" ? "justify-end" : ""}`}>
                {msg.role === "assistant" && (
                    <div className="w-8 h-8 rounded-full bg-[#4A8EF0] flex items-center justify-center text-white text-sm font-bold shrink-0">
                      M
                    </div>
                )}
                <div className={`rounded-2xl px-4 py-3 max-w-xs text-sm
                ${msg.role === "user"
                    ? "bg-[#4A8EF0] text-white rounded-tr-sm"
                    : `${getBubbleStyle(msg)} rounded-tl-sm shadow-sm`}`}>
                  {msg.content}
                </div>
              </div>
          ))}
          {loading && (
              <p className="text-xs text-gray-400 text-center">답변 생성 중...</p>
          )}
        </div>

        {/* 입력창 */}
        <div className="px-4 md:px-8 py-4 bg-white border-t border-gray-100">
          <div className="flex gap-2">
            <input
                type="text"
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && handleSend()}
                placeholder="메시지를 입력하세요..."
                className="flex-1 px-4 py-2.5 border border-gray-200 rounded-xl text-sm focus:outline-none focus:border-[#4A8EF0]"
            />
            <button
                onClick={handleSend}
                disabled={loading}
                className="px-5 py-2.5 bg-[#4A8EF0] text-white font-medium rounded-xl hover:bg-[#3a7ee0] transition-colors text-sm disabled:opacity-50">
              전송
            </button>
          </div>
        </div>
      </div>
  );
  }