"use client";
// 파일: src/components/AppNav.tsx
// 역할: (app) 라우트 그룹의 공통 네비게이션
//   - 모바일(<md): 하단 탭바 (가로 스크롤 + 좌우 화살표 버튼)
//   - 웹(md+): 좌측 사이드바 + 로그아웃 버튼
// 비고: 데스크톱은 마우스로 가로 스크롤이 어려워(스크롤바 숨김) 화살표 버튼으로 민다.
//       스크롤 위치에 따라 처음=> / 중간=<,> / 끝=< 만 보인다.

import { useEffect, useRef, useState, useCallback } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { LogOut, ChevronLeft, ChevronRight } from "lucide-react";
import { api } from "@/lib/api";

// 데스크톱 사이드바: 전체 항목 노출
const navItems = [
    { href: "/calendar", label: "캘린더", icon: "📅" },
    { href: "/diary/new", label: "기록", icon: "📝" },
    { href: "/chat", label: "대화", icon: "💬" },
    { href: "/report", label: "리포트", icon: "📊" },
    { href: "/analysis", label: "분석", icon: "📈" },
    { href: "/growth", label: "성장", icon: "🌱" },
    { href: "/settings", label: "설정", icon: "⚙️ " },
    { href: "/diary/search",  label: "검색",   icon: "🔍" },
];

// 모바일 하단 탭바: 핵심 6개만. 설정/검색은 사이드바에서만 접근.
const mobileNavItems = navItems.filter(
    (item) => item.href !== "/settings" && item.href !== "/diary/search"
);

export default function AppNav() {
    const pathname = usePathname();
    const router = useRouter();

    // 하단 탭바 가로 스크롤 상태
    const scrollRef = useRef<HTMLDivElement>(null);
    const [canLeft, setCanLeft] = useState(false);
    const [canRight, setCanRight] = useState(false);

    // 현재 스크롤 위치 기준으로 좌/우 화살표 표시 여부 갱신
    const updateArrows = useCallback(() => {
        const el = scrollRef.current;
        if (!el) return;
        setCanLeft(el.scrollLeft > 4);
        setCanRight(el.scrollLeft + el.clientWidth < el.scrollWidth - 4);
    }, []);

    useEffect(() => {
        const el = scrollRef.current;
        if (!el) return;
        updateArrows();
        // 창 크기 변화(사이드바<->탭바 전환, 폭 변경)와 요소 크기 변화 모두 감지
        const ro = new ResizeObserver(updateArrows);
        ro.observe(el);
        window.addEventListener("resize", updateArrows);
        return () => {
            ro.disconnect();
            window.removeEventListener("resize", updateArrows);
        };
    }, [updateArrows]);

    function scrollByDir(dir: 1 | -1) {
        scrollRef.current?.scrollBy({ left: dir * 160, behavior: "smooth" });
    }

    async function handleLogout() {
        try {
            // 백엔드가 HttpOnly 쿠키를 max-age=0으로 삭제해줌
            await api.post("/api/v1/auth/logout", undefined, { skipAuth: true });
        } catch {
            // 실패해도 로그인 페이지로 이동 (쿠키 만료까지 기다리지 않음)
        }
        router.replace("/login");
    }

    return (
        <>
            {/* 좌측 사이드바 (md 이상) */}
            <aside className="hidden md:flex flex-col w-60 min-h-screen bg-white border-r border-gray-100 px-4 py-6">
                <Link href="/calendar" className="mb-8 px-2 block">
                    <span className="text-xl font-bold text-[#4A8EF0]">Mind Compass</span>
                    <p className="text-xs text-gray-400 mt-1">감정나침반</p>
                </Link>
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
                    className="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium text-gray-400 hover:bg-gray-50 hover:text-gray-600 transition-colors">
                    <LogOut className="w-4 h-4" />
                    <span>로그아웃</span>
                </button>
            </aside>

            {/* 하단 탭바 (모바일, <md)
                각 항목 min-w-[130px](6개=780px) > md 분기(768px) → 항상 가로로 넘쳐 스크롤.
                마우스로는 스크롤바(숨김)를 못 쓰므로 좌우 화살표 버튼으로 민다. */}
            <div className="md:hidden fixed bottom-0 left-0 right-0 z-50">
                <div className="relative bg-white border-t border-gray-100">
                    {/* 스크롤 영역 */}
                    <div
                        ref={scrollRef}
                        onScroll={updateArrows}
                        className="flex overflow-x-auto [scrollbar-width:none] [&::-webkit-scrollbar]:hidden"
                    >
                        {mobileNavItems.map((item) => {
                            const isActive = pathname.startsWith(item.href);
                            return (
                                <Link
                                    key={item.href}
                                    href={item.href}
                                    className={`shrink-0 min-w-[130px] flex flex-col items-center justify-center py-3 text-xs font-medium transition-colors ${
                                        isActive ? "text-[#4A8EF0]" : "text-gray-400"
                                    }`}>
                                    <span className="text-lg mb-0.5">{item.icon}</span>
                                    <span>{item.label}</span>
                                </Link>
                            );
                        })}
                    </div>

                    {/* 왼쪽 화살표 — 왼쪽으로 더 스크롤 가능할 때만 */}
                    {canLeft && (
                        <button
                            type="button"
                            onClick={() => scrollByDir(-1)}
                            aria-label="이전 메뉴"
                            className="absolute left-0 top-0 bottom-0 flex items-center pl-1.5 pr-5 text-gray-500 bg-gradient-to-r from-white via-white to-transparent"
                        >
                            <ChevronLeft className="w-5 h-5" />
                        </button>
                    )}

                    {/* 오른쪽 화살표 — 오른쪽으로 더 스크롤 가능할 때만 */}
                    {canRight && (
                        <button
                            type="button"
                            onClick={() => scrollByDir(1)}
                            aria-label="다음 메뉴"
                            className="absolute right-0 top-0 bottom-0 flex items-center pr-1.5 pl-5 text-gray-500 bg-gradient-to-l from-white via-white to-transparent"
                        >
                            <ChevronRight className="w-5 h-5" />
                        </button>
                    )}
                </div>
            </div>
        </>
    );
}
