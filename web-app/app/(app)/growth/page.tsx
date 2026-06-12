"use client";
// 파일: app/(app)/growth/page.tsx
// 역할: '나의 성장 저장소' 화면 (Figma 신규 화면)
//   - 오늘의 마음 루틴 체크리스트
//   - 나를 살린 문장들(공감 기억) 말풍선
// 비고: 프론트 목업, 백엔드 무연동. 루틴/마음온도/공감기억은 하드코딩 목업 데이터.

import { useState } from "react";
import { useRouter } from "next/navigation";
import { ChevronLeft, Check } from "lucide-react";
import RoutineCompleteModal from "@/components/RoutineCompleteModal";

// 오늘의 마음 루틴 (목업) — done 토글로 완료 상태 관리
interface Routine {
    id: number;
    text: string;
    done: boolean;
}

const INITIAL_ROUTINES: Routine[] = [
    { id: 1, text: '퇴근길 나에게 "수고했어" 말하기', done: true },
    { id: 2, text: "5분간 창밖 풍경 바라보기", done: false },
];

// 나를 살린 문장들 (공감 기억, 목업) — AI 상담에서 저장된 문장
const MEMORIES = [
    {
        id: 1,
        text: '"오늘 발표는 결과와 상관없이, 준비했던 당신의 마음만으로도 충분히 빛나는 시간이었어요."',
        date: "5월 7일 AI 상담 중",
        tone: "warm" as const,
    },
    {
        id: 2,
        text: '"불안함은 당신이 그만큼 그 일을 진심으로 대하고 있다는 증거예요."',
        date: "5월 3일 AI 상담 중",
        tone: "cool" as const,
    },
];

export default function GrowthPage() {
    const router = useRouter();
    const [routines, setRoutines] = useState<Routine[]>(INITIAL_ROUTINES);
    const [showModal, setShowModal] = useState(false);

    // 미완 루틴 클릭 → 완료 토글 + 달성 팝업
    function completeRoutine(id: number) {
        setRoutines((prev) =>
            prev.map((r) => (r.id === id ? { ...r, done: true } : r))
        );
        setShowModal(true);
    }

    return (
        <div className="p-4 md:p-8 max-w-2xl mx-auto w-full">

            {/* 헤더 */}
            <div className="flex items-center gap-2 mb-4">
                <button
                    onClick={() => router.back()}
                    className="text-gray-700 hover:text-gray-900"
                    aria-label="뒤로"
                >
                    <ChevronLeft className="w-6 h-6" />
                </button>
                <h1 className="text-xl font-bold text-gray-800">나의 성장 저장소</h1>
            </div>

            {/* 오늘의 마음 루틴 */}
            <h2 className="text-base font-semibold text-gray-800 mt-4 mb-3">오늘의 마음 루틴</h2>
            <div className="flex flex-col gap-3">
                {routines.map((r) => (
                    <button
                        key={r.id}
                        onClick={() => !r.done && completeRoutine(r.id)}
                        disabled={r.done}
                        className={`flex items-center gap-3 w-full text-left rounded-2xl bg-white ring-1 ring-gray-100 shadow-sm p-4 transition-colors ${
                            r.done ? "cursor-default" : "hover:bg-gray-50 cursor-pointer"
                        }`}
                    >
                        {r.done ? (
                            <span className="flex items-center justify-center w-8 h-8 rounded-full bg-[#4A8EF0] shrink-0">
                                <Check className="w-5 h-5 text-white" strokeWidth={3} />
                            </span>
                        ) : (
                            <span className="w-8 h-8 rounded-full border-2 border-gray-300 shrink-0" />
                        )}
                        <span
                            className={`font-medium ${r.done ? "text-gray-800" : "text-gray-400"}`}
                        >
                            {r.text}
                        </span>
                    </button>
                ))}
            </div>

            {/* 나를 살린 문장들 (공감 기억) */}
            <h2 className="text-base font-bold text-gray-800 mt-6 mb-3">나를 살린 문장들 (공감 기억)</h2>
            <div className="flex flex-col gap-3">
                {MEMORIES.map((m) => (
                    <div
                        key={m.id}
                        className={`rounded-2xl p-4 ${
                            m.tone === "warm" ? "bg-[#FEF3E2]" : "bg-[#E8F0FE]"
                        }`}
                    >
                        {m.tone === "warm" && (
                            <span className="block text-2xl leading-none text-[#F59E0B] mb-1">❝</span>
                        )}
                        <p className="text-sm text-gray-700 leading-relaxed">{m.text}</p>
                        <p
                            className={`text-xs text-right mt-2 ${
                                m.tone === "warm" ? "text-[#F59E0B]" : "text-[#4A8EF0]"
                            }`}
                        >
                            {m.date}
                        </p>
                    </div>
                ))}
            </div>

            {/* 루틴 달성 완료 팝업 */}
            {showModal && <RoutineCompleteModal onClose={() => setShowModal(false)} />}
        </div>
    );
}
