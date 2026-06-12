"use client";
// 파일: app/(app)/chat/page.tsx
// 역할: AI 채팅 - 세션 목록 + 대화 화면 ("마음 대화")
// 호출: POST /chat/sessions, POST /chat/sessions/{id}/messages → backend-api → ai-api
// 비고: 디자인 Figma 기준 폴리시. 세션/전송/종료/안전 분기 로직은 보존.
//       응답의 responseType를 받아 SAFETY/SUPPORTIVE 버블을 색으로 구분(안전 분기 시각화).

import { useState, useEffect, useRef } from "react";
import { ChevronLeft, Plus, Send } from "lucide-react";
import { api } from "@/lib/api";

type ResponseType = "NORMAL" | "SAFETY" | "SUPPORTIVE" | "FALLBACK";

type Message = {
    id: number;
    role: "user" | "assistant";
    content: string;
    responseType?: ResponseType;
};

type ChatSession = {
    id: number;
    title: string;
    status: "ACTIVE" | "CLOSED";
    createdAt: string;
};

// responseType → 인트로 배지 스타일
const RESPONSE_BADGE: Record<ResponseType, { label: string; cls: string }> = {
    NORMAL:     { label: "NORMAL",     cls: "bg-[#4A8EF0]/10 text-[#4A8EF0]" },
    SUPPORTIVE: { label: "SUPPORTIVE", cls: "bg-[#34D399]/15 text-[#2F855A]" },
    SAFETY:     { label: "SAFETY",     cls: "bg-[#F87171]/15 text-[#C53030]" },
    FALLBACK:   { label: "FALLBACK",   cls: "bg-gray-100 text-gray-500" },
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

    async function selectSession(session: ChatSession) {
        const msgs = await api.get<Message[]>(`/api/v1/chat/sessions/${session.id}`);
        setSessionId(session.id);
        setMessages(Array.isArray(msgs) ? msgs : []);
        setView("chat");
    }

    async function startNewSession() {
        const res = await api.post<{ id: number }>("/api/v1/chat/sessions", { title: "새 상담" });
        setSessions((prev) => [
            { id: res.id, title: "새 상담", status: "ACTIVE", createdAt: new Date().toISOString() },
            ...prev,
        ]);
        setSessionId(res.id);
        setMessages([{ id: 0, role: "assistant", content: "안녕하세요.\n오늘 가장 마음에 남는 감정부터\n천천히 말해주셔도 괜찮아요." }]);
        setView("chat");
    }

    async function handleSend() {
        if (!input.trim() || !sessionId || loading) return;

        const userMsg: Message = { id: Date.now(), role: "user", content: input };
        setMessages((prev) => [...prev, userMsg]);
        setInput("");
        setLoading(true);

        try {
            const res = await api.post<{ id: number; role: string; content: string; responseType?: ResponseType }>(
                `/api/v1/chat/sessions/${sessionId}/messages`,
                { content: input }
            );
            setMessages((prev) => [...prev, { id: res.id, role: "assistant", content: res.content, responseType: res.responseType }]);
        } finally {
            setLoading(false);
        }
    }

    async function handleClose() {
        if (!sessionId) return;
        await api.post(`/api/v1/chat/sessions/${sessionId}/close`, {});
        setSessions((prev) => prev.map((s) => s.id === sessionId ? { ...s, status: "CLOSED" as const } : s));
        setView("list");
    }

    // assistant 버블 스타일 (안전 분기 색 구분)
    const getBubbleStyle = (msg: Message) => {
        if (msg.responseType === "SAFETY")     return "bg-[#FDECEC] border border-[#F6BDBD] text-[#C53030]";
        if (msg.responseType === "SUPPORTIVE") return "bg-[#FEF6E0] border border-[#F6E2A8] text-gray-700";
        if (msg.responseType === "FALLBACK")   return "bg-gray-50 border border-gray-200 text-gray-500";
        return "bg-white border border-gray-100 text-gray-700";
    };
    // assistant 아바타 색 (SUPPORTIVE/ SAFETY는 톤 다르게)
    const avatarStyle = (msg: Message) => {
        if (msg.responseType === "SAFETY")     return "bg-[#F87171]/15 text-[#C53030]";
        if (msg.responseType === "SUPPORTIVE") return "bg-[#34D399]/20 text-[#2F855A]";
        return "bg-[#4A8EF0]/15 text-[#4A8EF0]";
    };

    // 인트로 카드 배지 = 마지막 assistant 메시지의 responseType
    const lastType = [...messages].reverse().find((m) => m.role === "assistant")?.responseType;

    // ── 세션 목록 화면 ──────────────────────────────────────
    if (view === "list") {
        return (
            <div className="flex flex-col h-full max-w-2xl mx-auto w-full">
                <header className="px-4 md:px-8 py-4 flex items-center justify-between">
                    <div>
                        <h1 className="text-xl font-bold text-gray-800">마음 대화</h1>
                        <p className="text-xs text-gray-400 mt-0.5">이전 대화를 이어가거나 새로 시작하세요</p>
                    </div>
                    <button
                        onClick={startNewSession}
                        aria-label="새 대화"
                        className="w-10 h-10 rounded-full bg-[#4A8EF0]/10 text-[#4A8EF0] flex items-center justify-center hover:bg-[#4A8EF0]/20 transition-colors"
                    >
                        <Plus className="w-5 h-5" />
                    </button>
                </header>
                <div className="flex-1 overflow-y-auto p-4 md:p-8 flex flex-col gap-3">
                    {sessionsLoading && <p className="text-center text-sm text-gray-400 py-8">불러오는 중...</p>}
                    {!sessionsLoading && sessions.length === 0 && (
                        <div className="text-center py-16">
                            <p className="text-gray-400 text-sm mb-4">아직 대화 기록이 없어요</p>
                            <button
                                onClick={startNewSession}
                                className="rounded-full bg-[#4A8EF0] text-white text-sm font-semibold px-6 py-3 hover:bg-[#3a7ee0] transition-colors"
                            >
                                첫 대화 시작하기
                            </button>
                        </div>
                    )}
                    {sessions.map((session) => (
                        <button
                            key={session.id}
                            onClick={() => selectSession(session)}
                            className="w-full text-left bg-white rounded-2xl ring-1 ring-gray-100 shadow-sm px-4 py-4 hover:bg-[#4A8EF0]/5 transition-colors"
                        >
                            <div className="flex items-center justify-between">
                                <span className="text-sm font-semibold text-gray-800">{session.title}</span>
                                <span className={`text-xs px-2.5 py-0.5 rounded-full ${
                                    session.status === "ACTIVE" ? "bg-[#34D399]/15 text-[#2F855A]" : "bg-gray-100 text-gray-400"
                                }`}>
                                    {session.status === "ACTIVE" ? "진행 중" : "종료"}
                                </span>
                            </div>
                            <p className="text-xs text-gray-400 mt-1">{new Date(session.createdAt).toLocaleDateString("ko-KR")}</p>
                        </button>
                    ))}
                </div>
            </div>
        );
    }

    // ── 채팅 화면 ───────────────────────────────────────────
    return (
        <div className="flex flex-col h-full max-w-2xl mx-auto w-full">
            {/* 헤더 */}
            <header className="relative flex items-center justify-center px-4 md:px-8 py-4">
                <button onClick={() => setView("list")} className="absolute left-4 md:left-8 text-gray-700 hover:text-gray-900" aria-label="목록">
                    <ChevronLeft className="w-6 h-6" />
                </button>
                <h1 className="text-lg font-bold text-gray-800">마음 대화</h1>
                <div className="absolute right-4 md:right-8 flex items-center gap-2">
                    <button onClick={handleClose} className="text-xs text-gray-400 hover:text-gray-600">종료</button>
                    <button
                        onClick={startNewSession}
                        aria-label="새 대화"
                        className="w-9 h-9 rounded-full bg-[#4A8EF0]/10 text-[#4A8EF0] flex items-center justify-center hover:bg-[#4A8EF0]/20 transition-colors"
                    >
                        <Plus className="w-5 h-5" />
                    </button>
                </div>
            </header>

            <div className="flex-1 overflow-y-auto px-4 md:px-8 pb-4 flex flex-col gap-4">
                {/* 인트로 카드 */}
                <div className="rounded-2xl bg-[#EEF3FE] p-5">
                    <h2 className="text-lg font-bold text-gray-800">오늘 감정 상담</h2>
                    <p className="text-sm text-gray-500 mt-1">지금 마음 상태에 따라 일반 응답, 지원형 응답, 안전 안내로 분기돼요.</p>
                    {lastType && (
                        <span className={`inline-block mt-3 text-xs font-bold rounded-md px-2.5 py-1 ${RESPONSE_BADGE[lastType].cls}`}>
                            {RESPONSE_BADGE[lastType].label}
                        </span>
                    )}
                </div>

                {/* 메시지 */}
                {messages.map((msg) => (
                    <div key={msg.id} className={`flex gap-2.5 items-start ${msg.role === "user" ? "justify-end" : ""}`}>
                        {msg.role === "assistant" && (
                            <div className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold shrink-0 ${avatarStyle(msg)}`}>
                                AI
                            </div>
                        )}
                        <div className={`rounded-2xl px-4 py-3 max-w-[78%] text-sm whitespace-pre-wrap leading-relaxed ${
                            msg.role === "user"
                                ? "bg-[#4A8EF0] text-white rounded-tr-sm"
                                : `${getBubbleStyle(msg)} rounded-tl-sm`
                        }`}>
                            {msg.content}
                        </div>
                    </div>
                ))}
                {loading && <p className="text-xs text-gray-400 text-center">답변 생성 중...</p>}

                {/* 안전 분기 설명 (정보 카드) */}
                <div className="rounded-2xl border border-dashed border-[#F6E2A8] bg-[#FEF6E0]/60 p-4">
                    <p className="text-sm font-bold text-gray-700">위험 신호가 높게 감지되면</p>
                    <p className="text-sm text-gray-500 mt-1">일반 답변 대신 안전 안내 문구가 우선 제공됩니다.</p>
                    <p className="text-sm font-semibold text-gray-600 mt-1">예: 상담센터, 신뢰할 수 있는 사람에게 도움 요청</p>
                </div>

                <div ref={messagesEndRef} />
            </div>

            {/* 입력바 */}
            <div className="px-4 md:px-8 py-4">
                <div className="flex items-center gap-2 bg-gray-100 rounded-full pl-5 pr-2 py-2">
                    <textarea
                        value={input}
                        onChange={(e) => setInput(e.target.value)}
                        onKeyDown={(e) => {
                            if (e.key === "Enter" && !e.shiftKey) {
                                e.preventDefault();
                                handleSend();
                            }
                        }}
                        placeholder="지금 마음을 이야기해보세요..."
                        rows={1}
                        className="flex-1 bg-transparent text-sm focus:outline-none resize-none placeholder:text-gray-400 py-1"
                    />
                    <button
                        onClick={handleSend}
                        disabled={loading}
                        aria-label="전송"
                        className="w-10 h-10 rounded-full bg-[#4A8EF0] text-white flex items-center justify-center hover:bg-[#3a7ee0] transition-colors disabled:opacity-50 shrink-0"
                    >
                        <Send className="w-4 h-4" />
                    </button>
                </div>
            </div>
        </div>
    );
}
