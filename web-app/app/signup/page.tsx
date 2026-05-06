"use client";
// 파일: app/signup/page.tsx
// 역할: 회원가입 페이지 (인증 없이 접근 가능)
// 호출: POST /api/v1/auth/signup → backend-api

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { api } from "@/lib/api";

export default function SignupPage() {
  const router = useRouter();
  const [nickname, setNickname] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setErrorMessage("");

    if (password !== confirmPassword) {
      setErrorMessage("비밀번호가 일치하지 않습니다.");
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
      <div className="min-h-screen flex items-center justify-center bg-[#F8FAFC]">
        <Card className="w-full max-w-sm shadow-sm">
          <CardHeader className="text-center pb-2">
            <h1 className="text-2xl font-bold text-[#4A8EF0]">Mind Compass</h1>
            <p className="text-gray-400 text-sm">감정나침반</p>
            <h2 className="text-xl font-semibold text-gray-800 pt-2">회원가입</h2>
          </CardHeader>

          <CardContent>
            <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="nickname">닉네임</Label>
                <Input
                    id="nickname"
                    type="text"
                    placeholder="닉네임을 입력하세요 (2~50자)"
                    value={nickname}
                    onChange={(e) => setNickname(e.target.value)}
                    minLength={2}
                    maxLength={50}
                    required
                />
              </div>

              <div className="flex flex-col gap-1.5">
                <Label htmlFor="email">이메일</Label>
                <Input
                    id="email"
                    type="email"
                    placeholder="이메일을 입력하세요"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    required
                />
              </div>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="password">비밀번호</Label>
                <Input
                    id="password"
                    type="password"
                    placeholder="8~20자로 입력하세요"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    minLength={8}
                    maxLength={20}
                    required
                />
              </div>

              <div className="flex flex-col gap-1.5">
                <Label htmlFor="confirmPassword">비밀번호 확인</Label>
                <Input
                    id="confirmPassword"
                    type="password"
                    placeholder="비밀번호를 다시 입력하세요"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    required
                />
              </div>

              {errorMessage && (
                  <p className="text-red-500 text-sm">{errorMessage}</p>
              )}

              <Button
                  type="submit"
                  disabled={isLoading}
                  className="w-full bg-[#4A8EF0] hover:bg-[#3a7ee0]"
              >
                {isLoading ? "가입 중..." : "가입하기"}
              </Button>
            </form>

            <p className="text-center text-sm text-gray-500 mt-6">
              이미 계정이 있으신가요?{" "}
              <a href="/login" className="text-[#4A8EF0] font-medium">
                로그인
              </a>
            </p>
          </CardContent>
        </Card>
      </div>
  );
}