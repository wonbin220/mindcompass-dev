"use client";
// 파일: src/components/AppNav.tsx
// 역할: (app) 라우트 그룹의 공통 네비게이션
//   - 모바일: 하단 탭바 (캘린더, 기록, 대화, 리포트)
//   - 웹(md+): 좌측 사이드바 + 로그아웃 버튼

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { LogOut } from "lucide-react";

const navItems = [
    { href: "/calendar", label: "캘린더", icon: "📅" },
    { href: "/diary/new", label: "기록", icon: "📝" },
    { href: "/chat", label: "대화", icon: "💬" },
    { href: "/report", label: "리포트", icon: "📊" },
];

export default function AppNav() {
    const pathname = usePathname();
    const router = useRouter();

    function handleLogout() {
        // 1. localStorage 토큰 삭제
        localStorage.removeItem("access_token");

        // 2. cookie 만료 (max-age=0으로 즉시 삭제)
        document.cookie = "access_token=; path=/; max-age=0; SameSite=Strict";

        // 3. 로그인 페이지로 이동
        router.replace("/login");
    }
    return (
        <>
            {/* 좌측 사이드바 (md 이상) */}
            <aside className="hidden md:flex flex-col w-60 min-h-screen bg-white border-r border-gray-100 px-4 py-6">
                <div className="mb-8 px-2">
                    <span className="text-xl font-bold text-[#4A8EF0]">Mind Compass</span>
                    <p className="text-xs text-gray-400 mt-1">감정나침반</p>
                </div>
                <nav className="flex flex-col gap-1 flex-1">
                    {navItems.map((item) => {
                        const isActive = pathname.startsWith(item.href);
                        return (
                            <Link
                                key={item.href}
                                href={item.href}
                                className={`flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                                    isActive
                                        ? "bg-[#4A8EF0]/10 text-[#4A8EF0]"
                                        : "text-gray-600 hover:bg-gray-50"
                                }`}
                            >
                                <span>{item.icon}</span>
                                <span>{item.label}</span>
                            </Link>
                        );
                    })}
                </nav>

                {/* 로그아웃 버튼 — 사이드바 맨 아래 */}
                <button
                    onClick={handleLogout}
                    className="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium text-gray-400 hover:bg-gray-50
   hover:text-gray-600 transition-colors"
                >
                    <LogOut className="w-4 h-4" />
                    <span>로그아웃</span>
                </button>
            </aside>

            {/* 하단 탭바 (모바일) */}
            <nav className="md:hidden fixed bottom-0 left-0 right-0 bg-white border-t border-gray-100 flex z-50">
                {navItems.map((item) => {
                    const isActive = pathname.startsWith(item.href);
                    return (
                        <Link
                            key={item.href}
                            href={item.href}
                            className={`flex-1 flex flex-col items-center justify-center py-3 text-xs font-medium transition-colors
  ${
                                isActive ? "text-[#4A8EF0]" : "text-gray-400"
                            }`}
                        >
                            <span className="text-lg mb-0.5">{item.icon}</span>
                            <span>{item.label}</span>
                        </Link>
                    );
                })}
            </nav>
        </>
    );
}
