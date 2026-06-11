"use client";
// 파일: app/(app)/layout.tsx
// 역할: 인증이 필요한 페이지들의 공통 레이아웃 (네비게이션 포함)
// 인증 가드는 middleware.ts(서버, access_token 쿠키 검사)가 담당한다.
// HttpOnly 쿠키는 JS로 못 읽으므로 클라이언트에서 localStorage로 토큰을 확인하지 않는다.
// 하위 페이지가 useSearchParams를 쓰므로 {children}을 Suspense로 감싸 프리렌더 CSR 충돌을 방지한다.

import { Suspense } from "react";
import AppNav from "@/components/AppNav";

export default function AppLayout({
                                    children,
                                }: {
    children: React.ReactNode;
}) {
    return (
        <div className="flex min-h-screen bg-[#F8FAFC]">
            <AppNav />
            <main className="flex-1 flex flex-col pb-16 md:pb-0">
                <Suspense fallback={null}>{children}</Suspense>
            </main>
        </div>
    );
}