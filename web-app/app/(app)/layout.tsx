"use client";
// 파일: app/(app)/layout.tsx
// 역할: 인증이 필요한 페이지들의 공통 레이아웃 (네비게이션 포함)
// 호출: calendar, diary, chat, report 페이지

import { useEffect, useState } from "react";                                                                 [15/1159]
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
            setIsReady(true);
        }
    }, []);

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
