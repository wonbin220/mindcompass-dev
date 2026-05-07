import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
    title: "Mind Compass",
    description: "감정나침반",
};

export default function RootLayout({
                                       children,
                                   }: {
    children: React.ReactNode;
}) {
    return (
        <html lang="ko">
        <body>{children}</body>
        </html>
    );
}
