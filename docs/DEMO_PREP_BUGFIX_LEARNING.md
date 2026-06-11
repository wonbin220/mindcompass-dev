<!--
파일: DEMO_PREP_BUGFIX_LEARNING.md
역할: 2026-06-11/12 데모 리허설 중 발견한 버그들의 원인·수정·대안·예방 기록
대상: junior backend/frontend 개발자
-->

# 데모 준비 디버깅 기록 (2026-06-11/12)

데모 시연 리허설을 하다가 **로그인부터 막혔고**, 파고들어 보니 프론트엔드·백엔드·인프라에 걸쳐 **버그 5개 + 미해결 1개**가 있었다. 이 문서는 각각의 **증상 → 원인 → 어떻게 고쳤나 → 다른 방법은 없었나 → 예방책**을 정리한다.

핵심 교훈을 먼저 말하면:
- **curl로 200이 떠도 "동작한다"가 아니다.** curl은 JS를 실행하지 않으므로 프론트엔드 버그를 못 잡는다.
- **추측보다 증거.** 처음 세운 가설(라우터 캐시)이 틀렸고, 브라우저 네트워크 로그·서버 로그를 직접 본 뒤에야 진짜 원인(localStorage)을 찾았다.
- **push 전 로컬 빌드.** 클라우드 빌드 실패를 3번 반복한 뒤에야 로컬 `npm run build`로 먼저 검증하는 습관을 들였다.

---

## 한눈에 보기

| # | 증상 | 진짜 원인 | 수정 | 커밋 |
|---|------|----------|------|------|
| 1 | 로그인하면 캘린더 잠깐 떴다 `/login`으로 튕김 | `(app)/layout.tsx`가 `localStorage`로 토큰 확인 (인증은 HttpOnly 쿠키 → JS가 못 읽음 → 항상 null) | localStorage 가드 제거, 인증은 middleware에 위임 | `dc9622f` |
| 2 | web-app 빌드 실패 (`useSearchParams ... suspense`) | 1번 수정으로 프리렌더가 살아나며 잠복 버그 노출 | `(app)/layout.tsx`에서 `{children}`을 `<Suspense>`로 감쌈 | `aabcb20` |
| 3 | 새 일기에 AI 감정이 `null`로 저장됨 | backend→ai-api 타임아웃 5초인데 ai-api fallback이 6~11초 → 타임아웃·재시도 후 결과 유실 | `AI_API_TIMEOUT_MS` 기본값 5000→15000 | `6328619` |
| 4 | AI가 감정을 분석해도 캘린더 이모지가 안 뜸 | `applyAnalysis`가 aiAnalysis/emotionTags엔 저장하나 `diary.primaryEmotion`은 미반영 | `Diary.applyAiEmotionIfAbsent()` 추가, 사용자 미선택 시 AI 감정 반영 | `8b1f6fc` |
| 5 | fastapi `emotion-classify`가 매 요청 500 (미해결) | 시작 시 S3 모델 다운로드 `NoCredentialsError`(task에 S3 권한 없음) + `sentencepiece` 미설치 | sentencepiece 추가는 했으나 S3 권한 미해결 → ai-api keyword fallback으로 데모 우회 | `7c3b33e` |

---

## 1. 로그인 후 `/login`으로 무한 튕김

### 증상
시크릿 창에서 로그인 → 캘린더가 **잠깐 보였다가 곧바로 로그인 화면으로** 돌아감. 무한 반복.

### 진단 과정 (중요)
1. `curl`로 로그인 API 직접 호출 → `200` + `Set-Cookie`(access/refresh) 정상. **백엔드는 멀쩡.**
2. 쿠키 jar로 속성 확인 → `HttpOnly; Secure; SameSite=Lax; Path=/` 전부 정상.
3. 쿠키 들고 `/calendar` 문서 요청 → `200`. **서버 인증 체인도 정상.**
4. 그런데 브라우저는 튕김 → "쿠키도 인증도 정상인데 왜?" → **클라이언트(JS)에서 돌려보내는 것**으로 좁혀짐.
5. 처음엔 "Next.js 라우터 캐시"로 **잘못 추측** → `window.location.href` 수정 배포 → **여전히 튕김** → 가설 폐기.
6. 보호 페이지 공통 레이아웃 `(app)/layout.tsx`를 읽음 → 범인 발견.

### 진짜 원인
```js
// web-app/app/(app)/layout.tsx (수정 전)
useEffect(() => {
  const token = localStorage.getItem("access_token");  // ← 항상 null
  if (!token) router.replace("/login");                  // ← 매번 튕김
  else setIsReady(true);
}, []);
```
이 프로젝트의 인증은 보안상 **HttpOnly 쿠키**다. HttpOnly의 정의가 **"JavaScript가 이 쿠키를 못 읽는다"**(XSS 토큰 탈취 방어). 그러니 `localStorage.getItem("access_token")`은 **영원히 null** → 로그인 성공(쿠키 발급)과 무관하게 매번 `/login`으로 돌려보냄. 페이지가 마운트되는 순간 useEffect가 실행되니, 캘린더가 잠깐 떴다가 사라지는 증상이 된다. `localStorage`는 web-app 전체에서 이 한 곳에서만 쓰였고 어디서도 set하지 않았다 → 옛 localStorage 기반 인증 설계의 잔재.

### 어떻게 고쳤나
보호 경로(`/calendar`, `/diary`, `/chat`, `/report`, `/settings`)는 이미 **`middleware.ts`가 서버에서 access_token 쿠키로 가드**하고 있었다. 따라서 클라이언트 레이아웃의 인증 체크는 **틀렸을 뿐 아니라 불필요**. localStorage 체크·`isReady` 게이트를 통째로 제거하고 children만 렌더하게 바꿈.

### 다른 방법은 없었나
- **(택 가능) 클라이언트에서 `GET /api/v1/users/me`로 인증 확인** 후 401이면 리다이렉트 — HttpOnly 쿠키는 fetch에 자동 동봉되므로 가능. 하지만 middleware가 이미 서버에서 막으므로 **중복**이고 요청만 늘어남. 그래서 제거가 정답.
- **(반패턴) 토큰을 localStorage에 저장** — 그러면 JS로 읽을 수 있지만 **XSS에 토큰이 노출**된다. 보안 설계를 깨는 선택이라 기각.

### 예방책
- **HttpOnly 쿠키 인증에선 클라이언트에서 토큰 존재를 확인하지 말 것.** 가드는 서버(middleware/필터)가 단독으로.
- 인증 방식을 바꿀 때(localStorage→쿠키) **모든 토큰 접근 지점을 grep으로 전수 확인** (`grep -rn localStorage`).
- E2E를 **실제 브라우저**로도 한 번 돌릴 것. curl은 이 버그를 못 잡는다.

---

## 2. `useSearchParams` 프리렌더 빌드 깨짐

### 증상
1번 수정을 배포하니 **CI 빌드 실패**: `useSearchParams() should be wrapped in a suspense boundary at page "/diary/new"`.

### 원인
옛 레이아웃은 `if (!isReady) return null`로 **빌드 시 프리렌더 단계에서 children을 아예 안 그렸다.** 그래서 `/diary/*` 페이지들의 `useSearchParams()`가 프리렌더 때 호출되지 않아 에러가 **숨어 있었다.** 1번에서 그 `return null` 게이트를 없애니 children이 프리렌더되며 잠복 버그가 드러난 것. (해당 3개 페이지: `/diary`, `/diary/search`, `/diary/new`)

### 어떻게 고쳤나
**한 곳으로 해결**: `(app)/layout.tsx`에서 `{children}`을 `<Suspense>`로 감쌌다. Suspense 경계는 **조상 어디에 있어도** 그 아래 모든 `useSearchParams`를 커버하므로, 레이아웃 한 곳이면 하위 전체가 해결된다.

### 다른 방법은 없었나
- **`export const dynamic = "force-dynamic"`** 을 각 페이지에 추가 — **시도했으나 실패.** Next 15에서 이 route segment config는 **`"use client"` 페이지에선 무시**된다. (이걸 모르고 넣었다가 빌드 1회 더 깨짐)
- **각 페이지를 개별 `<Suspense>`로 래핑** (login 페이지가 쓰는 방식) — 동작하지만 페이지마다 컴포넌트를 둘로 쪼개야 해서 번거롭다. 레이아웃 한 곳 래핑이 더 간결.

### 예방책
- **`useSearchParams`/`usePathname` 쓰는 클라이언트 페이지는 Suspense 경계 필요** (Next.js App Router 정적 프리렌더 제약).
- **공통 레이아웃에서 `{children}`을 Suspense로 감싸두면** 하위 페이지가 무엇을 쓰든 안전.
- **push 전 로컬 `npm run build`로 검증** — 이걸 안 해서 클라우드 빌드를 3번 반복했다.

---

## 3. 새 일기에 AI 감정이 `null`로 저장됨

### 증상
일기를 쓰면 `201`로 저장은 되는데, 잠시 뒤 조회해도 `primaryEmotion: null`, `aiAnalysis: null`. AI 분석이 안 붙음.

### 진단
ai-api 로그를 보니 일기 1건당 분석 요청이 **3번** 찍히고, 매번 fastapi 500 후 fallback으로 `emotion=피로`까지 **완료**되는데도 결과가 일기에 반영 안 됨. 일기 생성 요청이 **~17초** 걸린 것이 단서.

### 원인
```yaml
# backend application.yml
ai.api.timeout-ms: ${AI_API_TIMEOUT_MS:5000}   # 5초
```
backend→ai-api WebClient 타임아웃이 **5초**(connect/response 공통) + 재시도 2회. 그런데 ai-api 분석은 fastapi 500 → keyword fallback → 위험도 분석 → OpenAI 요약까지 **6~11초** 걸린다. 5초에 끊겨 재시도 3회 모두 타임아웃 → backend가 결과를 못 받아 **null로 마감.** ai-api는 일을 끝냈지만 backend가 이미 포기한 상태.

### 어떻게 고쳤나
타임아웃 기본값을 **5000→15000(15초)**으로. `application.yml`의 `${AI_API_TIMEOUT_MS:5000}` 한 숫자만 변경(prod는 이 값을 따로 안 덮어쓰고 상속). 결과: 일기 생성 ~8초에 fallback 결과(`emotion=피로` + OpenAI 요약)가 도착해 정상 저장.

### 다른 방법은 없었나
- **ECS 환경변수 `AI_API_TIMEOUT_MS=15000`** 추가 (코드/빌드 없이) — 가능했으나, **버전관리되는 설정 변경**이 포트폴리오엔 더 깔끔해서 yaml 수정 택함.
- **AI 분석을 비동기(@Async)로** 빼서 일기 저장 응답을 즉시 반환하고 분석은 백그라운드로 — **더 나은 설계**지만(저장 응답 ~8초 지연 제거) 변경 범위가 커서 데모용으론 보류. **촬영 후 개선 후보.**
- **ai-api fallback을 더 빠르게** (OpenAI 요약 생략 등) — 응답 품질 trade-off라 기각.

### 예방책
- **타임아웃은 "정상 경로"가 아니라 "최악 경로(fallback 포함)" 시간 + 여유**로 잡을 것.
- **동기 후처리는 사용자 응답을 지연**시킨다 → 무거운 AI 후처리는 비동기로 빼는 게 정석.
- 외부/내부 호출엔 **타임아웃·재시도·fallback**을 셋트로, 그리고 실제 소요시간을 로그로 관측.

---

## 4. AI 감정이 캘린더 이모지로 안 뜸

### 증상
3번을 고쳐 AI 감정이 `aiAnalysis`엔 들어가는데, **캘린더·목록 이모지는 여전히 안 뜸.** (시드 일기는 떴음)

### 원인
캘린더/리포트는 `diary.primaryEmotion`(엔티티 자체 필드)을 읽는다. 그런데 `DiaryService.applyAnalysis()`는 AI 결과를 **`DiaryAiAnalysis` 레코드 + `emotionTags`에만** 저장하고 **`diary.primaryEmotion`은 안 건드렸다.** 설계상 `diary.primaryEmotion`은 **사용자가 직접 고른 감정**이고, 데모에선 감정을 안 골라서 null. 시드 일기는 이 필드가 직접 박혀 있어 떠 있었던 것(버그가 가려져 있었음).

### 어떻게 고쳤나
`Diary`에 메서드 추가 후 `applyAnalysis`에서 호출:
```java
// 사용자가 직접 고른 감정이 있으면 덮어쓰지 않는다
public void applyAiEmotionIfAbsent(String primaryEmotion, Integer emotionIntensity) {
    if (this.primaryEmotion == null || this.primaryEmotion.isBlank()) {
        this.primaryEmotion = primaryEmotion;
        this.emotionIntensity = emotionIntensity;
    }
}
```
**사용자 선택이 있으면 존중하고, 없을 때만 AI 감정으로 채운다.** JPA dirty checking으로 트랜잭션 커밋 시 자동 반영.

### 다른 방법은 없었나
- **무조건 AI 감정으로 덮어쓰기** — 사용자가 직접 고른 감정을 무시하게 되어 기각.
- **응답 DTO/프론트가 `aiAnalysis.primaryEmotion`을 fallback으로 읽게** — 프론트 여러 곳을 고쳐야 해서 더 번거로움. 데이터 일관성 측면에서도 엔티티 필드를 채우는 게 깔끔.

### 예방책
- "표시용 대표값"과 "원천 분석 데이터"가 **다른 필드에 저장**될 때, **표시용 필드를 채우는 책임을 명확히** 할 것.
- 시드 데이터가 실제 코드 경로를 거치지 않고 직접 박히면 **버그가 가려진다** → 가능하면 시드도 실제 생성 흐름으로.

---

## 5. (미해결) fastapi 감정모델 500 — 데모는 fallback으로 우회

### 증상
ai-api 로그에 매번 `FastAPI 감정분류 실패: 500 ... /internal/model/emotion-classify`.

### 원인 (2겹)
fastapi 시작 로그가 결정적이었다:
```
[1/2] S3에서 tired_v5 모델 다운로드 중...
botocore.exceptions.NoCredentialsError: Unable to locate credentials   ← ①
...
ValueError: ... You need to have sentencepiece or tiktoken installed   ← ②
  at hybrid_predictor.py: AutoTokenizer.from_pretrained(TIRED_MODEL_PATH)
```
- **① S3 자격증명 없음**: fastapi ECS task에 **S3 read IAM 권한이 없어** tired_v5 모델 다운로드 실패. (06-10 "e2e 검증"은 로컬 Docker라 로컬 AWS 키로 됐던 것 → **클라우드에선 한 번도 안 떴음**)
- **② sentencepiece 미설치**: 토크나이저가 SentencePiece 기반인데 라이브러리가 없어 로딩 실패.

### 현재 처리 (데모용 우회)
`requirements.txt`에 `sentencepiece`+`protobuf`는 추가(배포)했으나 **① S3 권한은 미해결**이라 실모델은 여전히 안 뜬다. 대신 **ai-api의 keyword fallback**이 "피로" 등 합당한 감정을 반환하므로 **데모 동선은 정상 동작**한다. 이건 오히려 이 프로젝트의 safety-first(모델이 죽어도 fallback으로 흐름 유지) 설계를 라이브로 증명하는 그림이라, 촬영 멘트로 살릴 수 있다.

### 진짜 고치려면 (촬영 후)
- **fastapi ECS task role에 S3 GetObject 권한 부여** (모델 버킷 대상), 또는
- **모델 파일을 Docker 이미지에 베이킹**(런타임 S3 의존 제거), 그리고
- sentencepiece로 토크나이저 정상 로딩되는지 재검증.

### 예방책
- **로컬 Docker e2e ≠ 클라우드 e2e.** 로컬엔 개발자 AWS 키가 있어 S3가 되지만 ECS task role엔 없을 수 있다. **인프라 검증은 실제 클라우드에서.**
- 런타임에 외부(S3/HF)에서 자원을 받는 컨테이너는 **권한·네트워크를 task 단위로 명시**하고 시작 로그로 성공 확인.
- 모델처럼 필수 자산은 **이미지 베이킹 vs 런타임 다운로드** trade-off를 의식적으로 결정.

---

## 프로세스 교훈 (도구·방법)

1. **curl 200 ≠ 동작.** curl은 JS 미실행 → 프론트 인증/렌더 버그를 못 본다. 결정타는 **브라우저 Network/Application 탭**(요청 쿠키·상태코드·Application Cookies)이었다.
2. **추측을 증거로 교체.** 첫 가설(라우터 캐시)이 틀렸고 배포까지 했다. 서버 로그·브라우저 로그를 직접 본 뒤에야 진짜 원인을 잡았다. **틀린 가설은 빨리 폐기.**
3. **push 전 로컬 빌드 게이트.** 클라우드 빌드를 3번 깨먹은 뒤 `npm run build`로 먼저 검증 → 한 번에 통과. CI 사이클(수 분)을 로컬(수십 초)로 대체.
4. **AWS CLI 버전 함정.** 이 환경은 **CLI v1**이라 `aws logs tail`(v2 전용)이 안 됨 → `aws logs filter-log-events --log-group-name ... --start-time $(date -d '10 min ago' +%s)000` 사용. (데모 가이드의 해당 명령도 교체 필요)
5. **배포 검증은 "이미지 태그 = 커밋 SHA"로.** GitHub Actions가 `IMAGE_TAG=github.sha`로 빌드하므로, ECS task def의 이미지 태그가 내 커밋 SHA와 일치하는지로 "내 수정이 떴다"를 확정.

---

## 종합 예방 체크리스트

- [ ] HttpOnly 쿠키 인증: 클라이언트에서 토큰 존재 확인 금지, 가드는 서버 단독
- [ ] 인증 방식 변경 시 토큰 접근 지점 전수 grep
- [ ] 클라이언트 `useSearchParams` 페이지는 (공통 레이아웃 등에서) Suspense 경계 확보
- [ ] push 전 로컬 빌드(`npm run build`/`./gradlew build`)로 검증
- [ ] 내부 호출 타임아웃은 최악(fallback 포함) 경로 기준 + 여유, 무거운 후처리는 비동기 고려
- [ ] 표시용 대표 필드를 채우는 책임을 명확히 (분석결과 → 엔티티 반영)
- [ ] 인프라(S3 권한 등)는 로컬이 아니라 **실제 클라우드에서** 검증, 시작 로그로 성공 확인
- [ ] E2E는 curl + **실제 브라우저** 둘 다
- [ ] AWS CLI 버전에 맞는 명령 사용

---

## 다음 작업 (내일 이어서)
1. 데모 시연영상 촬영 (폰 너비 ~420px, 채팅 safety 분기가 클라이맥스)
2. 촬영 후 UI 마무리 (Figma 맞추기 — 신규화면 프론트목업 + 기존 폴리시), 시드 데이터 보강, 리포트 빈 AI박스 숨김
3. (선택) fastapi 실모델 살리기 (S3 IAM), AI 분석 비동기화
4. 자원 해제 (TEARDOWN_RUNBOOK)
