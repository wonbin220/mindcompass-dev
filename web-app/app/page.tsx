// 파일: app/page.tsx
// 역할: 루트 경로 → /calendar 로 리다이렉트
import { redirect } from "next/navigation";

export default function RootPage() {
  redirect("/calendar");
}
