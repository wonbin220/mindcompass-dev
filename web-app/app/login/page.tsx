"use client";
// 파일: app/login/page.tsx
// 역할: 로그인 페이지 (인증 없이 접근 가능)
// 호출: POST /api/v1/auth/login → backend-api

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { useSearchParams } from "next/navigation";
import { api } from "@/lib/api";

export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const searchParams = useSearchParams();

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setIsLoading(true);
    setErrorMessage("");

    try {
      // 백엔드가 Set-Cookie로 access_token, refresh_token을 심어줌
      // 응답 body에 토큰 없음 → 제네릭 타입 void
      await api.post<void>(
          "/api/v1/auth/login",
          { email, password },
          { skipAuth: true }
      );

      // open redirect 방어: "/"로 시작하고 "//"로 시작하지 않는 경로만 허용
      // 변경: from 파라미터가 있으면 거기로, 없으면 홈으로
      const from = searchParams.get("from") || "/";
      const safePath = from.startsWith("/") && !from.startsWith("//") ? from : "/";
      router.push(safePath);
    } catch {
      setErrorMessage("이메일 또는 비밀번호가 올바르지 않습니다.");
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
            <h2 className="text-xl font-semibold text-gray-800 pt-2">로그인</h2>
          </CardHeader>

          <CardContent>
            <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
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
                    placeholder="비밀번호를 입력하세요"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
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
                {isLoading ? "로그인 중..." : "로그인"}
              </Button>
            </form>

            <p className="text-center text-sm text-gray-500 mt-6">
              계정이 없으신가요?{" "}
              <a href="/signup" className="text-[#4A8EF0] font-medium">
                회원가입
              </a>
            </p>
          </CardContent>
        </Card>
      </div>
  );
}


