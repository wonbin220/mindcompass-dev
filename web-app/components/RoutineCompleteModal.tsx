"use client";
// 파일: components/RoutineCompleteModal.tsx
// 역할: '루틴 달성 완료' 축하 팝업 (Figma 신규 화면)
// 비고: 프론트 목업, 백엔드 무연동. growth 페이지에서 루틴 체크 시 표시.

interface RoutineCompleteModalProps {
    onClose: () => void;
}

export default function RoutineCompleteModal({ onClose }: RoutineCompleteModalProps) {
    return (
        // 전체 화면 dim 오버레이 — 바깥 클릭 시 닫힘
        <div
            className="fixed inset-0 bg-black/30 flex items-center justify-center z-50"
            onClick={onClose}
        >
            {/* 모달 카드 — 내부 클릭은 닫힘 전파 차단 */}
            <div
                className="bg-white rounded-2xl shadow-xl px-8 py-7 mx-6 max-w-xs w-full text-center"
                onClick={(e) => e.stopPropagation()}
            >
                <div className="w-20 h-20 rounded-full bg-[#FDEEDD] mx-auto flex items-center justify-center">
                    <span className="text-4xl" role="img" aria-label="축하">🎉</span>
                </div>

                <h2 className="text-xl font-bold text-gray-800 mt-4">루틴 달성 완료!</h2>
                <p className="text-gray-500 text-sm mt-1">마음 온도가 0.5도 올라갔어요.</p>

                <button
                    onClick={onClose}
                    className="mt-5 w-full bg-[#4A8EF0] hover:bg-[#3a7ee0] text-white font-bold rounded-full py-3 transition-colors"
                >
                    확인
                </button>
            </div>
        </div>
    );
}
