"use client";
// 파일: app/(app)/layout.tsx
// 역할: 인증이 필요한 페이지들의 공통 레이아웃
// 변경: 클라이언트 auth guard 추가

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import AppNav from "@/components/AppNav";

export default function AppLayout({
                                    children,
                                  }: {
  children: React.ReactNode;
}) {
  const router = useRouter();
  const [isReady, setIsReady] = useState(false);

  useEffect(() => {
    const token = localStorage.getItem("access_token");
    if (!token) {
      router.replace("/login");
    } else {
      setIsReady(true); // 토큰 있으면 화면 표시
    }
  }, []);

  // 토큰 확인 전엔 아무것도 렌더링하지 않음 (flash 방지)
  if (!isReady) return null;

  return (
      <div className="flex min-h-screen bg-[#F8FAFC]">
        <AppNav />
        <main className="flex-1 flex flex-col pb-16 md:pb-0">
          {children}
        </main>
      </div>
  );
}