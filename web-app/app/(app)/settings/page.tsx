"use client";
// 파일: app/(app)/settings/page.tsx
// 역할: 내 정보 조회 + 닉네임 수정 (GET/PATCH /api/v1/users/me 연동)

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import { useRouter } from "next/navigation";

interface UserMe {
    id: number;
    email: string;
    nickname: string;
    createdAt: string;
}

export default function SettingsPage() {
    const [user, setUser] = useState<UserMe | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState("");

    // 닉네임 수정 상태
    const [isEditing, setIsEditing] = useState(false);
    const [nicknameInput, setNicknameInput] = useState("");
    const [isSaving, setIsSaving] = useState(false);
    const [saveError, setSaveError] = useState("");

    const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
    const [isDeleting, setIsDeleting] = useState(false);
    const [deleteError, setDeleteError] = useState("");

    const router = useRouter();

    useEffect(() => {
        api.get<UserMe>("/api/v1/users/me")
            .then(setUser)
            .catch(() => setError("내 정보를 불러오지 못했습니다."))
            .finally(() => setIsLoading(false));
    }, []);

    function startEdit() {
        setNicknameInput(user?.nickname ?? "");
        setSaveError("");
        setIsEditing(true);
    }

    async function handleSave() {                                                                              [79/1491]
        if (nicknameInput.length < 2 || nicknameInput.length > 50) {
            setSaveError("닉네임은 2~50자여야 합니다.");
            return;
        }
        setIsSaving(true);
        setSaveError("");
        try {
            const updated = await api.patch<UserMe>("/api/v1/users/me", { nickname: nicknameInput });
            setUser(updated);
            setIsEditing(false);
        } catch {
            setSaveError("저장에 실패했습니다. 다시 시도해주세요.");
        } finally {
            setIsSaving(false);
        }
    }

    async function handleDeleteAccount() {
        setIsDeleting(true);
        setDeleteError("");
        try {
            await api.delete("/api/v1/users/me");
            // 탈퇴 완료 → 쿠키 삭제 후 로그인 페이지
            await api.post("/api/v1/auth/logout", undefined, { skipAuth: true });
            router.replace("/login");
        } catch {
            setDeleteError("탈퇴 처리에 실패했습니다. 다시 시도해주세요.");
            setIsDeleting(false);
        }
    }

    if (isLoading) {
        return <div className="flex items-center justify-center h-40 text-gray-400 text-sm">불러오는 중...</div>;
    }
    if (error) {
        return <div className="flex items-center justify-center h-40 text-red-400 text-sm">{error}</div>;
    }

    return (
        <div className="max-w-md mx-auto px-4 py-8">
            <h1 className="text-xl font-bold text-gray-800 mb-6">내 정보</h1>

            <div className="bg-white rounded-xl border border-gray-100 divide-y divide-gray-50">

                {/* 닉네임 */}
                <div className="px-4 py-4">
                    <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-400">닉네임</span>
                        {!isEditing && (
                            <button
                                onClick={startEdit}
                                className="text-xs text-[#4A8EF0] hover:underline"
                            >
                                수정
                            </button>
                        )}
                    </div>
                    {isEditing ? (
                        <div className="mt-2 flex flex-col gap-2">
                            <input
                                type="text"
                                value={nicknameInput}
                                onChange={(e) => setNicknameInput(e.target.value)}
                                maxLength={50}
                                className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:border-[#4A8EF0]"/>
                            {saveError && <p className="text-xs text-red-400">{saveError}</p>}
                            <div className="flex gap-2">
                                <button
                                    onClick={handleSave}
                                    disabled={isSaving}
                                    className="flex-1 py-2 bg-[#4A8EF0] text-white text-sm font-medium rounded-lg hover:bg-[#3a7ee0] disabled:opacity-50">
                                    {isSaving ? "저장 중..." : "저장"}
                                </button>
                                <button
                                    onClick={() => setIsEditing(false)}
                                    className="flex-1 py-2 border border-gray-200 text-sm text-gray-500 rounded-lg hover:bg-gray-50">
                                취소
                            </button>
                            </div>
                        </div>
                        ) : (
                        <p className="mt-1 text-sm font-medium text-gray-800">{user?.nickname}</p>
                )}
                </div>

                {/* 이메일 */}
                <div className="flex items-center justify-between px-4 py-4">
                    <span className="text-sm text-gray-400">이메일</span>
                    <span className="text-sm font-medium text-gray-800">{user?.email}</span>
                </div>

                {/* 가입일 */}
                <div className="flex items-center justify-between px-4 py-4">
                    <span className="text-sm text-gray-400">가입일</span>
                    <span className="text-sm font-medium text-gray-800">
                  {user?.createdAt ? new Date(user.createdAt).toLocaleDateString("ko-KR") : "-"}
                </span>
                </div>

                {/* 회원 탈퇴 */}
                <div className="mt-8">
                    {!showDeleteConfirm ? (
                        <button
                            onClick={() => setShowDeleteConfirm(true)}
                            className="text-sm text-red-400 hover:text-red-600 hover:underline">
                            회원 탈퇴
                        </button>
                    ) : (
                        <div className="bg-red-50 border border-red-200 rounded-xl px-4 py-4 flex flex-col gap-3">
                            <p className="text-sm text-red-700 font-medium">정말 탈퇴하시겠습니까?</p>
                            <p className="text-xs text-red-500">모든 일기와 대화 기록이 삭제되며 복구할 수 없습니다.</p>
                            {deleteError && <p className="text-xs text-red-600">{deleteError}</p>}
                            <div className="flex gap-2">
                                <button
                                    onClick={handleDeleteAccount}
                                    disabled={isDeleting}
                                    className="flex-1 py-2 bg-red-500 text-white text-sm font-medium rounded-lg hover:bg-red-600 disabled:opacity-50">
                                    {isDeleting ? "처리 중..." : "탈퇴하기"}
                                </button>
                                <button
                                    onClick={() => { setShowDeleteConfirm(false); setDeleteError(""); }}
                                    className="flex-1 py-2 border border-gray-200 text-sm text-gray-500 rounded-lg hover:bg-gray-50">
                                    취소
                                </button>
                            </div>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}
