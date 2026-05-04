// 파일: app/layout.tsx
// 역할: 전체 앱 루트 레이아웃 (공통 메타데이터 및 폰트 적용)
import type { Metadata } from "next";
import { Geist } from "next/font/google";
import "./globals.css";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "Mind Compass | 감정나침반",
  description: "AI 기반 멘탈 헬스 기록 및 상담 서비스",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko" className={`${geistSans.variable} h-full antialiased`}>
      <body className="min-h-full flex flex-col bg-[#F8FAFC]">{children}</body>
    </html>
  );
}
