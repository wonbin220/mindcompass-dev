"use client";
// 파일: app/(app)/chat/page.tsx
// 역할: AI 채팅 - 세션 목록 + 대화 화면
// 호출: POST /chat/sessions, POST /chat/sessions/{id}/messages → backend-api → ai-api

import { useState, useEffect, useRef } from "react";
import { api } from "@/src/lib/api";


type Message = {
  id: number;
  role: "user" | "assistant";
  content: string;
  responseType?: "NORMAL" | "SAFETY" | "SUPPORTIVE" | "FALLBACK";
};

type ChatSession = {
  id: number;
  title: string;
  status: "ACTIVE" | "CLOSED";
  createdAt: string;
};

export default function ChatPage() {

  const [view, setView] = useState<"list" | "chat">("list");
  const [sessions, setSessions] = useState<ChatSession[]>([]);
  const [sessionsLoading, setSessionsLoading] = useState(true);
  const [sessionId, setSessionId] = useState<number | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);

  const messagesEndRef = useRef<HTMLDivElement>(null);
  useEffect(() => {
    api.get<{ content: ChatSession[] }>("/api/v1/chat/sessions")
        .then((res) => setSessions(res.content ?? []))
        .finally(() => setSessionsLoading(false));
  }, []);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);


  // 기존 세션 선택 → 메시지 이어보기
  async function selectSession(session: ChatSession) {
    const msgs = await api.get<Message[]>(`/api/v1/chat/sessions/${session.id}`);
    setSessionId(session.id);
    setMessages(Array.isArray(msgs) ? msgs : []);
    setView("chat");
  }

  // 새 세션 생성
  async function startNewSession() {
    const res = await api.post<{ id: number }>("/api/v1/chat/sessions", {
      title: "새 상담",
    });
    setSessions((prev) => [
      {id: res.id, title: "새 상담", status: "ACTIVE", createdAt: new Date().toISOString()},
      ...prev,
    ]);
    setSessionId(res.id);
    setMessages([{id: 0, role: "assistant", content: "안녕하세요. 오늘 어떠셨나요? 편하게 이야기해주세요."}]);
    setView("chat");
  }

  async function handleSend() {
    if (!input.trim() || !sessionId || loading) return;

    const userMsg: Message = {id: Date.now(), role: "user", content: input};
    setMessages((prev) => [...prev, userMsg]);
    setInput("");
    setLoading(true);

    try {
      const res = await api.post<{ id: number; role: string; content: string }>(
          `/api/v1/chat/sessions/${sessionId}/messages`,
          {content: input}
      );
      setMessages((prev) => [...prev, {id: res.id, role: "assistant", content: res.content}]);
    } finally {
      setLoading(false);
    }
  }

  async function handleClose() {
    if (!sessionId) return;
    await api.post(`/api/v1/chat/sessions/${sessionId}/close`, {});
    // 목록에서 해당 세션 상태를 CLOSED로 업데이트
    setSessions((prev) =>
        prev.map((s) => s.id === sessionId ? { ...s, status: "CLOSED" as const } : s)
    );
    setView("list");
  }

  const getBubbleStyle = (msg: Message) => {
    if (msg.responseType === "SAFETY")     return "bg-red-50 border border-red-200 text-red-700";
    if (msg.responseType === "SUPPORTIVE") return "bg-amber-50 border border-amber-200 text-amber-700";
    if (msg.responseType === "FALLBACK")   return "bg-gray-50 border border-gray-200 text-gray-500";
    return "bg-white border border-gray-100 text-gray-700";
  };

// ── 세션 목록 화면 ──────────────────────────────────────
  if (view === "list") {
    return (
        <div className="flex flex-col h-full max-w-2xl mx-auto w-full">
          <header className="px-4 md:px-8 py-4 bg-white border-b border-gray-100 flex items-center justify-between">
            <div>
              <h1 className="text-xl font-bold text-gray-800">AI 대화</h1>
              <p className="text-xs text-gray-400 mt-0.5">이전 대화를 이어가거나 새로 시작하세요</p>
            </div>
            <button
                onClick={startNewSession}
                className="px-4 py-2 bg-[#4A8EF0] text-white text-sm font-medium rounded-xl hover:bg-[#3a7ee0] transition-colors">
              + 새 대화
            </button>
          </header>
          <div className="flex-1 overflow-y-auto p-4 md:p-8 flex flex-col gap-3">
            {sessionsLoading && (
                <p className="text-center text-sm text-gray-400 py-8">불러오는 중...</p>
            )}
            {!sessionsLoading && sessions.length === 0 && (
                <div className="text-center py-16">
                  <p className="text-gray-400 text-sm mb-4">아직 대화 기록이 없어요</p>
                  <button
                      onClick={startNewSession}
                      className="px-5 py-2.5 bg-[#4A8EF0] text-white text-sm font-medium rounded-xl hover:bg-[#3a7ee0] transition-colors"
                  >
                    첫 대화 시작하기
                  </button>
                </div>
            )}
            {sessions.map((session) => (
                <button
                    key={session.id}
                    onClick={() => selectSession(session)}
                    className="w-full text-left bg-white border border-gray-100 rounded-xl px-4 py-4 hover:border-[#4A8EF0]/30 hover:bg-[#4A8EF0]/5 transition-colors"
                >
                  <div className="flex items-center justify-between">
                    <span className="text-sm font-medium text-gray-800">{session.title}</span>
                    <span className={`text-xs px-2 py-0.5 rounded-full ${
                        session.status === "ACTIVE"
                            ? "bg-green-50 text-green-600"
                            : "bg-gray-50 text-gray-400"
                    }`}>
                    {session.status === "ACTIVE" ? "진행 중" : "종료"}
                  </span>
                  </div>
                  <p className="text-xs text-gray-400 mt-1">
                    {new Date(session.createdAt).toLocaleDateString("ko-KR")}
                  </p>
                </button>
            ))}
          </div>
        </div>
    );
  }

// ── 채팅 화면 ───────────────────────────────────────────
  return (
      <div className="flex flex-col h-full max-w-2xl mx-auto w-full">
        <header className="px-4 md:px-8 py-4 bg-white border-b border-gray-100 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <button
                onClick={() => setView("list")}
                className="text-gray-400 hover:text-gray-600 transition-colors text-sm">
              ← 목록
            </button>
            <h1 className="text-xl font-bold text-gray-800">AI 대화</h1>
          </div>
          <button
              onClick={handleClose}
              className="text-xs text-gray-400 hover:text-red-400 transition-colors border border-gray-200 px-3 py-1.5 rounded-lg hover:border-red-200">
            대화 종료
          </button>
        </header>

        <div className="flex-1 overflow-y-auto p-4 md:p-8 flex flex-col gap-4">
          {messages.map((msg) => (
              <div key={msg.id} className={`flex gap-3 items-start ${msg.role === "user" ? "justify-end" : ""}`}>
                {msg.role === "assistant" && (
                    <div
                        className="w-8 h-8 rounded-full bg-[#4A8EF0] flex items-center justify-center text-white text-sm font-bold shrink-0">
                      M
                    </div>
                )}
                <div className={`rounded-2xl px-4 py-3 max-w-xs text-sm ${
                    msg.role === "user"
                        ? "bg-[#4A8EF0] text-white rounded-tr-sm"
                        : `${getBubbleStyle(msg)} rounded-tl-sm shadow-sm`
                }`}>
                  {msg.content}
                </div>
              </div>
          ))}
          {loading && <p className="text-xs text-gray-400 text-center">답변 생성 중...</p>}
          <div ref={messagesEndRef} />
        </div>

        <div className="px-4 md:px-8 py-4 bg-white border-t border-gray-100">
          <div className="flex gap-2">
           <textarea
               value={input}
               onChange={(e) => setInput(e.target.value)}
               onKeyDown={(e) => {
                 if (e.key === "Enter" && !e.shiftKey) {
                   e.preventDefault();
                   handleSend();
                 }
               }}
               placeholder="메시지를 입력하세요... (Shift+Enter: 줄바꿈)"
               rows={1}
               className="flex-1 px-4 py-2.5 border border-gray-200 rounded-xl text-sm focus:outline-none focus:border-[#4A8EF0] resize-none"/>
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

