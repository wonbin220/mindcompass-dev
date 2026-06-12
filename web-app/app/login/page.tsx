"use client";
// 파일: app/login/page.tsx
// 역할: 로그인 페이지 (인증 없이 접근 가능)
// 호출: POST /api/v1/auth/login → backend-api
// 비고: 디자인은 Figma(감정나침반) 기준. 인증 로직은 그대로, 표현만 폴리시.
//       '로그인 상태 유지'/'비밀번호 찾기'는 현재 UI 목업(백엔드 미연동).

import { useState, Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { Input } from "@/components/ui/input";
import { api } from "@/lib/api";

// 감정나침반 로고 — Figma의 8방향 별표(나침반 로즈) 모양
function CompassMark({ className }: { className?: string }) {
    return (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor"
             strokeWidth={2} strokeLinecap="round" className={className}>
            <path d="M12 3.5V20.5" />
            <path d="M3.5 12H20.5" />
            <path d="M6.3 6.3 17.7 17.7" />
            <path d="M17.7 6.3 6.3 17.7" />
        </svg>
    );
}

// 입력칸 공통 스타일 — Figma의 채워진 연회색·둥근·큰 필드
const FIELD_CLASS =
    "h-14 rounded-xl border-0 bg-gray-100 px-4 text-base placeholder:text-gray-400 focus-visible:ring-2 focus-visible:ring-[#4A8EF0]/40";

function LoginForm() {
    const router = useRouter();
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [rememberMe, setRememberMe] = useState(true);
    const [isLoading, setIsLoading] = useState(false);
    const [errorMessage, setErrorMessage] = useState("");
    const searchParams = useSearchParams();

    async function handleSubmit(e: React.FormEvent) {
        e.preventDefault();
        setIsLoading(true);
        setErrorMessage("");

        try {
            await api.post<void>(
                "/api/v1/auth/login",
                { email, password },
                { skipAuth: true }
            );

            const from = searchParams.get("from") || "/";
            const safePath = from.startsWith("/") && !from.startsWith("//") ? from : "/";
            window.location.href = safePath; // ← 풀 로드 → middleware 새로 실행 → 캐시 우회
        } catch {
            setErrorMessage("이메일 또는 비밀번호가 올바르지 않습니다.");
        } finally {
            setIsLoading(false);
        }
    }

    return (
        <div className="min-h-screen flex items-center justify-center bg-[#F8FAFC] px-6 py-10">
            <div className="w-full max-w-sm">

                {/* 로고 + 타이틀 */}
                <div className="flex flex-col items-center text-center mb-8">
                    <div className="w-16 h-16 rounded-full bg-[#4A8EF0]/10 flex items-center justify-center">
                        <CompassMark className="w-8 h-8 text-[#4A8EF0]" />
                    </div>
                    <h1 className="text-3xl font-bold text-gray-800 mt-3">감정나침반</h1>
                    <p className="text-gray-400 text-sm mt-1">하루의 감정과 회복 흐름을 함께 기록해요</p>
                </div>

                {/* 로그인 폼 */}
                <form className="flex flex-col gap-5" onSubmit={handleSubmit}>
                    <div className="flex flex-col gap-2">
                        <label htmlFor="email" className="text-sm font-semibold text-gray-800">이메일</label>
                        <Input
                            id="email"
                            type="email"
                            placeholder="example@email.com"
                            className={FIELD_CLASS}
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            required
                        />
                    </div>

                    <div className="flex flex-col gap-2">
                        <label htmlFor="password" className="text-sm font-semibold text-gray-800">비밀번호</label>
                        <Input
                            id="password"
                            type="password"
                            placeholder="8자 이상 입력해주세요"
                            className={FIELD_CLASS}
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                        />
                    </div>

                    {/* 로그인 상태 유지 / 비밀번호 찾기 */}
                    <div className="flex items-center justify-between">
                        <button
                            type="button"
                            onClick={() => setRememberMe((v) => !v)}
                            className="flex items-center gap-2 text-sm text-gray-600"
                        >
                            <span
                                className={`flex items-center justify-center w-5 h-5 rounded-full border transition-colors ${
                                    rememberMe ? "bg-[#4A8EF0] border-[#4A8EF0]" : "border-gray-300"
                                }`}
                            >
                                {rememberMe && <span className="w-2 h-2 rounded-full bg-white" />}
                            </span>
                            로그인 상태 유지
                        </button>
                        <button type="button" className="text-sm font-semibold text-[#4A8EF0]">
                            비밀번호 찾기
                        </button>
                    </div>

                    {errorMessage && (
                        <p className="text-red-500 text-sm">{errorMessage}</p>
                    )}

                    {/* 로그인 버튼 */}
                    <button
                        type="submit"
                        disabled={isLoading}
                        className="w-full rounded-full bg-[#4A8EF0] hover:bg-[#3a7ee0] text-white font-bold py-4 text-base transition-colors disabled:opacity-60"
                    >
                        {isLoading ? "로그인 중..." : "로그인"}
                    </button>
                </form>

                {/* 또는 구분선 */}
                <div className="flex items-center gap-3 my-6">
                    <span className="flex-1 h-px bg-gray-200" />
                    <span className="text-xs text-gray-400">또는</span>
                    <span className="flex-1 h-px bg-gray-200" />
                </div>

                {/* 회원가입 버튼 */}
                <button
                    type="button"
                    onClick={() => router.push("/signup")}
                    className="w-full rounded-full border border-[#4A8EF0] text-[#4A8EF0] font-semibold py-4 bg-white hover:bg-[#4A8EF0]/5 transition-colors"
                >
                    회원가입
                </button>

                {/* 하단 안내 */}
                <p className="text-center text-xs text-gray-400 mt-6">
                    처음이라면 감정 기록부터 시작해보세요
                </p>
            </div>
        </div>
    );
}

export default function LoginPage() {
    return (
        <Suspense>
            <LoginForm />
        </Suspense>
    );
}
