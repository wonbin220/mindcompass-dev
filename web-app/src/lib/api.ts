// 파일: src/lib/api.ts
// 역할: backend-api 호출 래퍼 — HttpOnly 쿠키 기반, Next.js proxy 경유
// 호출: 각 페이지/컴포넌트 → api.ts → /api/** (Next.js proxy) → backend-api

async function refreshAccessToken(): Promise<boolean> {
  try {
    const response = await fetch("/api/v1/auth/refresh", {
      method: "POST",
      credentials: "include",  // refresh_token 쿠키 자동 전송
    });
    return response.ok;  // 성공하면 백엔드가 새 access_token 쿠키를 Set-Cookie로 내려줌
  } catch {
    return false;
  }
}

interface RequestOptions extends RequestInit {
  skipAuth?: boolean;
}

async function apiFetch<T>(
    path: string,
    options: RequestOptions = {},
    isRetry = false
): Promise<T> {
  const { skipAuth = false, ...fetchOptions } = options;

  const response = await fetch(path, {
    ...fetchOptions,
    credentials: "include",  // 쿠키 자동 포함
    headers: {
      "Content-Type": "application/json",
      ...(fetchOptions.headers as Record<string, string>),
    },
  });

  if (!response.ok) {
    // 401 + 첫 시도 → refresh 시도 후 재시도
    if (response.status === 401 && !isRetry && !skipAuth) {
      const refreshed = await refreshAccessToken();
      if (refreshed) {
        return apiFetch<T>(path, options, true);
      }
    }
    // refresh 실패 또는 재시도 후에도 401 → 로그인 페이지
    if (response.status === 401) {
      window.location.href = "/login";
      throw new Error("인증이 만료되었습니다.");
    }

    const errorBody = await response.text();
    throw new Error(`API 오류 ${response.status}: ${errorBody}`);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const json = await response.json();

  if (json && typeof json === "object" && "data" in json) {
    return json.data as T;
  }
  return json as T;
}

export const api = {
  get: <T>(path: string, options?: RequestOptions) =>
      apiFetch<T>(path, { method: "GET", ...options }),

  post: <T>(path: string, body?: unknown, options?: RequestOptions) =>
      apiFetch<T>(path, {
        method: "POST",
        body: body !== undefined ? JSON.stringify(body) : undefined,
        ...options,
      }),

  patch: <T>(path: string, body: unknown, options?: RequestOptions) =>
      apiFetch<T>(path, {
        method: "PATCH",
        body: JSON.stringify(body),
        ...options,
      }),

  delete: <T>(path: string, options?: RequestOptions) =>
      apiFetch<T>(path, { method: "DELETE", ...options }),
};




