"use client";
// 파일: app/signup/page.tsx
// 역할: 회원가입 페이지 ("반가워요!", 인증 없이 접근 가능)
// 호출: POST /api/v1/auth/signup → backend-api
// 비고: 디자인 Figma 기준 폴리시. signup 호출 로직은 보존.
//       비밀번호 확인 필드는 Figma에 없어 제거. 약관 동의는 클라이언트 게이트(백엔드 미저장).

import { useState } from "react";
import { useRouter } from "next/navigation";
import { ChevronLeft, Check } from "lucide-react";
import { Input } from "@/components/ui/input";
import { api } from "@/lib/api";

const FIELD_CLASS =
    "h-14 rounded-xl border-0 bg-gray-100 px-4 text-base placeholder:text-gray-400 focus-visible:ring-2 focus-visible:ring-[#4A8EF0]/40";

export default function SignupPage() {
    const router = useRouter();
    const [nickname, setNickname] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [agreePrivacy, setAgreePrivacy] = useState(false);
    const [agreeTerms, setAgreeTerms] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
    const [errorMessage, setErrorMessage] = useState("");

    async function handleSubmit(e: React.FormEvent) {
        e.preventDefault();
        setErrorMessage("");

        if (!agreePrivacy || !agreeTerms) {
            setErrorMessage("필수 약관에 모두 동의해주세요.");
            return;
        }

        setIsLoading(true);
        try {
            await api.post(
                "/api/v1/auth/signup",
                { email, password, nickname },
                { skipAuth: true }
            );
            router.push("/login");
        } catch {
            setErrorMessage("이미 사용 중인 이메일입니다.");
        } finally {
            setIsLoading(false);
        }
    }

    return (
        <div className="min-h-screen bg-[#F8FAFC] px-6 py-8">
            <div className="w-full max-w-sm mx-auto">

                {/* 헤더 */}
                <button onClick={() => router.push("/login")} className="text-gray-700 hover:text-gray-900 mb-4" aria-label="뒤로">
                    <ChevronLeft className="w-6 h-6" />
                </button>
                <h1 className="text-3xl font-bold text-gray-800">반가워요!</h1>
                <p className="text-gray-400 text-sm mt-2 mb-8">마음 기록을 위한 계정을 만들어주세요.</p>

                <form className="flex flex-col gap-5" onSubmit={handleSubmit}>
                    <div className="flex flex-col gap-2">
                        <label htmlFor="nickname" className="text-sm font-bold text-[#4A8EF0]">닉네임</label>
                        <Input
                            id="nickname"
                            type="text"
                            placeholder="익명으로 활동해도 좋아요"
                            className={FIELD_CLASS}
                            value={nickname}
                            onChange={(e) => setNickname(e.target.value)}
                            minLength={2}
                            maxLength={50}
                            required
                        />
                    </div>

                    <div className="flex flex-col gap-2">
                        <label htmlFor="email" className="text-sm font-bold text-[#4A8EF0]">이메일</label>
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
                        <label htmlFor="password" className="text-sm font-bold text-[#4A8EF0]">비밀번호</label>
                        <Input
                            id="password"
                            type="password"
                            placeholder="8자 이상 입력해주세요"
                            className={FIELD_CLASS}
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            minLength={8}
                            maxLength={20}
                            required
                        />
                    </div>

                    {/* 약관 동의 */}
                    <div className="flex flex-col gap-3 mt-1">
                        {[
                            { checked: agreePrivacy, set: setAgreePrivacy, label: "개인정보 수집 및 이용 동의 (필수)" },
                            { checked: agreeTerms, set: setAgreeTerms, label: "서비스 이용약관 동의 (필수)" },
                        ].map((row) => (
                            <button
                                key={row.label}
                                type="button"
                                onClick={() => row.set((v) => !v)}
                                className="flex items-center gap-2.5 text-left"
                            >
                                <span
                                    className={`flex items-center justify-center w-6 h-6 rounded-full transition-colors shrink-0 ${
                                        row.checked ? "bg-[#4A8EF0]" : "bg-gray-200"
                                    }`}
                                >
                                    <Check className="w-3.5 h-3.5 text-white" strokeWidth={3} />
                                </span>
                                <span className={`text-sm ${row.checked ? "text-gray-700" : "text-gray-400"}`}>{row.label}</span>
                            </button>
                        ))}
                    </div>

                    {errorMessage && <p className="text-red-500 text-sm">{errorMessage}</p>}

                    {/* 시작하기 */}
                    <button
                        type="submit"
                        disabled={isLoading}
                        className="w-full rounded-full bg-[#4A8EF0] hover:bg-[#3a7ee0] text-white font-bold py-4 text-base transition-colors disabled:opacity-60 mt-3"
                    >
                        {isLoading ? "가입 중..." : "감정나침반 시작하기"}
                    </button>
                </form>
            </div>
        </div>
    );
}
