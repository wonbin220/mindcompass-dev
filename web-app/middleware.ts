// 파일: middleware.ts
// 역할: 보호된 경로 접근 시 토큰 확인 → 없으면 /login으로 redirect
// 실행: 서버에서 요청마다 실행됨 (페이지 렌더링 전)

import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

export function middleware(request: NextRequest) {
    const token = request.cookies.get("access_token")?.value;
    const { pathname } = request.nextUrl;

    // 토큰 없이 보호 경로 접근 → /login 으로 redirect
    if (!token) {
        const loginUrl = new URL("/login", request.url);
        loginUrl.searchParams.set("from", pathname); // 로그인 후 원래 페이지로 돌아가기
        return NextResponse.redirect(loginUrl);
    }

    return NextResponse.next(); // 토큰 있으면 통과
}

// 이 경로에서만 middleware 실행
export const config = {
    matcher: [
        "/calendar/:path*",
        "/diary/:path*",
        "/chat/:path*",
        "/report/:path*",
    ],
};
