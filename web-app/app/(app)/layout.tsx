// 파일: app/(app)/layout.tsx
// 역할: 인증이 필요한 페이지들의 공통 레이아웃 (네비게이션 포함)
// 호출: calendar, diary, chat, report 페이지

import AppNav from "@/components/AppNav";

export default function AppLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="flex min-h-screen bg-[#F8FAFC]">
      <AppNav />
      {/* 메인 콘텐츠 영역 */}
      <main className="flex-1 flex flex-col pb-16 md:pb-0">
        {children}
      </main>
    </div>
  );
}
