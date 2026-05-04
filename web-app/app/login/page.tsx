"use client";
// 파일: app/login/page.tsx
// 역할: 로그인 페이지 (인증 없이 접근 가능)
// 호출: POST /auth/login → backend-api

export default function LoginPage() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-[#F8FAFC]">
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-8 w-full max-w-sm">
        <div className="text-center mb-8">
          <h1 className="text-2xl font-bold text-[#4A8EF0]">Mind Compass</h1>
          <p className="text-gray-400 text-sm mt-1">감정나침반</p>
        </div>

        <h2 className="text-xl font-semibold text-gray-800 mb-6">로그인</h2>

        <form className="flex flex-col gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              이메일
            </label>
            <input
              type="email"
              placeholder="이메일을 입력하세요"
              className="w-full px-4 py-2.5 border border-gray-200 rounded-lg text-sm focus:outline-none focus:border-[#4A8EF0]"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              비밀번호
            </label>
            <input
              type="password"
              placeholder="비밀번호를 입력하세요"
              className="w-full px-4 py-2.5 border border-gray-200 rounded-lg text-sm focus:outline-none focus:border-[#4A8EF0]"
            />
          </div>

          <button
            type="submit"
            className="w-full py-3 bg-[#4A8EF0] text-white font-medium rounded-lg hover:bg-[#3a7ee0] transition-colors mt-2"
          >
            로그인
          </button>
        </form>

        <p className="text-center text-sm text-gray-500 mt-6">
          계정이 없으신가요?{" "}
          <a href="/signup" className="text-[#4A8EF0] font-medium">
            회원가입
          </a>
        </p>
      </div>
    </div>
  );
}
