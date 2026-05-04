"use client";
// 파일: app/(app)/chat/page.tsx
// 역할: AI 채팅 페이지 - 상담형 대화 (protected)
// 호출: POST /chat/sessions, POST /chat/sessions/{id}/messages → backend-api → ai-api

export default function ChatPage() {
  return (
    <div className="flex flex-col h-full max-w-2xl mx-auto w-full">
      <header className="px-4 md:px-8 py-4 bg-white border-b border-gray-100">
        <h1 className="text-xl font-bold text-gray-800">AI 대화</h1>
        <p className="text-xs text-gray-400 mt-0.5">마음을 편하게 이야기해보세요</p>
      </header>

      {/* 메시지 목록 */}
      <div className="flex-1 overflow-y-auto p-4 md:p-8 flex flex-col gap-4">
        {/* AI 메시지 */}
        <div className="flex gap-3 items-start">
          <div className="w-8 h-8 rounded-full bg-[#4A8EF0] flex items-center justify-center text-white text-sm font-bold shrink-0">
            M
          </div>
          <div className="bg-white rounded-2xl rounded-tl-sm px-4 py-3 shadow-sm border border-gray-100 max-w-xs">
            <p className="text-sm text-gray-700">
              안녕하세요. 오늘 어떠셨나요? 편하게 이야기해주세요.
            </p>
          </div>
        </div>

        {/* 사용자 메시지 (예시) */}
        <div className="flex gap-3 items-start justify-end">
          <div className="bg-[#4A8EF0] rounded-2xl rounded-tr-sm px-4 py-3 max-w-xs">
            <p className="text-sm text-white">
              오늘 좀 힘들었어요...
            </p>
          </div>
        </div>

        {/* SAFETY 분기 안내 메시지 placeholder */}
        <p className="text-center text-xs text-gray-300">
          위험 신호 감지 시 안전 응답으로 자동 분기됩니다
        </p>
      </div>

      {/* 메시지 입력창 */}
      <div className="px-4 md:px-8 py-4 bg-white border-t border-gray-100">
        <div className="flex gap-2">
          <input
            type="text"
            placeholder="메시지를 입력하세요..."
            className="flex-1 px-4 py-2.5 border border-gray-200 rounded-xl text-sm focus:outline-none focus:border-[#4A8EF0]"
          />
          <button className="px-5 py-2.5 bg-[#4A8EF0] text-white font-medium rounded-xl hover:bg-[#3a7ee0] transition-colors text-sm">
            전송
          </button>
        </div>
      </div>
    </div>
  );
}
