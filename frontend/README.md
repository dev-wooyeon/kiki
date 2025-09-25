# KIKI — 게임 업데이트 요약 메일러 (MVP)

게임 공지를 찾아다니지 않아도 됩니다. KIKI가 주기적으로 공지 변화를 감지하고, 아침에 한 통의 메일로 핵심만 정리해 드립니다.

> [!NOTE]
> 이 저장소는 단일 레포(monorepo)입니다. `frontend/`는 Next.js, `backend/`는 Kotlin/Spring Boot로 구성되어 있습니다.

## 주요 기능
- 자동 수집·요약: 공지 변화를 주기적으로(기본 30분) 감지하고 요약 메일 발송
- 데일리 다이제스트: 게임별 그룹화 + 링크 모음, 선택적으로 LLM 요약 포함
- 구독/취소: 랜딩 페이지에서 이메일만으로 구독, 메일 하단 링크로 취소
- 키보드 중심 구독 UX(데스크톱)
  - Shift+Q/W/E: 니케/마비노기 모바일/명조 토글
  - Shift+Enter: 즉시 제출
  - 페이지 진입 시 입력창 자동 포커스(데스크톱)
- 라이트/다크 테마: 상단 토글 버튼으로 전환
- 모니터링: 헬스/스케줄/수동 스크래핑/강제 발송 엔드포인트 제공

> [!TIP]
> 운영에서는 특정 게임명을 문구에 드러내지 않고 “실시간 감지 → 요약 메일” 가치에 집중한 슬로건을 권장합니다.

---

## 프런트엔드 아키텍처 & 디렉터리
```
frontend/
├─ src/
│  ├─ app/                # Next App Router, layout.tsx, globals.css
│  ├─ components/         # UI 컴포넌트(아토믹)
│  │  ├─ LandingShell.tsx # 페이지 골격(헤더/섹션/푸터)
│  │  ├─ HeroSection.tsx  # 히어로 텍스트/구독자 배지
│  │  ├─ SubscriptionForm.tsx # 구독 폼 + 단축키 + 칩
│  │  ├─ IconHighlights.tsx   # 기능 카드(자동 수집/요약 메일)
│  │  ├─ InfoPanel.tsx    # (선택) 추가 정보 모달
│  │  ├─ Logo.tsx         # 로고(‘키키’)
│  │  └─ BackgroundStars.tsx # 다크 모드 배경
│  ├─ lib/
│  │  └─ api.ts           # axios 클라이언트, 구독자 수 API
│  └─ fonts/              # Pretendard Variable
└─ package.json
```

### 상태/상호작용
- ThemeProvider로 라이트/다크 토글, CSS 변수 기반 테마
- SubscriptionForm
  - Shift+Q/W/E: 선호 게임 칩 토글(데스크톱만 노출)
  - Shift+Enter: 제출, 입력칸 우측에 성공/오류 아이콘 노출
  - 제출 성공 시 구독자 수 즉시 재조회(옵티미스틱 +1 fallback)

---

## 기술 스택
- Frontend: Next.js 15, React 19, Tailwind v4, Framer Motion
- Backend: Spring Boot 3 (Kotlin 1.9), JPA/Hibernate, Jsoup, JavaMailSender
- DB: MySQL(운영), H2(로컬 프로필)
- Infra(권장): Vercel(프런트), Fly.io(백엔드), PlanetScale(서버리스 MySQL)

---

## 빠른 시작(로컬)
### 1) 백엔드
```bash
# 백엔드 디렉터리로 이동
cd backend

# 로컬 프로필(H2)로 실행
./gradlew bootRun -Dspring.profiles.active=local

# 테스트
./gradlew test
```
로컬 메일 발송을 위해 `backend/src/main/resources/application-local.yml`에 Gmail 앱 비밀번호/발신 정보를 채워주세요.

### 2) 프런트엔드
```bash
cd frontend
npm i
# API 베이스 경로(로컬 백엔드)
export NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api
npm run dev
```

> [!IMPORTANT]
> CORS: 백엔드 `application.yml`의 `spring.web.cors.allowed-origins`를 `http://localhost:3000` 등 프런트 도메인으로 맞춰야 합니다.

---

## API 요약(주요 엔드포인트)
- 구독: `POST /api/subscribe` { email }
- 구독 취소: `GET /api/unsubscribe/{token}`
- 구독 통계: `GET /api/subscribe/stats` → { activeCount }
- 모니터링
  - `GET /api/monitoring/health`
  - `GET /api/monitoring/schedule`
  - `POST /api/monitoring/scrape` (수동 스크래핑)
  - `POST /api/monitoring/notifications?force=true&hours=24` (전송 이력 무시 강제 발송)

> [!CAUTION]
> 강제 발송은 모니터링·QA 용도입니다. 운영에서는 큐/율제어를 반드시 고려하세요.

---

## 배포(권장 템플릿)
### 프런트 — Vercel
- 프로젝트 루트: `frontend`
- Environment Variables
  - `NEXT_PUBLIC_API_BASE_URL=https://<backend-domain>/api`
- 프레임워크는 자동 인식(Next.js). `vercel.json` 템플릿 포함.

### 백엔드 — Fly.io
```bash
cd backend
flyctl launch --no-deploy --name kiki-backend --region nrt
# 필수 시크릿 등록(예시)
flyctl secrets set \
  DB_USERNAME=... DB_PASSWORD=... \
  DB_URL='jdbc:mysql://aws.connect.psdb.cloud/<db>?useSSL=true&enabledTLSProtocols=TLSv1.2' \
  EMAIL_FROM=noreply@kiki.com \
  MAIL_USERNAME=... MAIL_PASSWORD=... \
  ALLOWED_ORIGINS=https://<vercel-domain> \
  OPENAI_API_KEY=sk-...
flyctl deploy
```
- `backend/Dockerfile`·`backend/fly.toml` 포함.

### 데이터베이스 — PlanetScale(권장)
- DB 생성 → JDBC 연결정보 발급 → Fly secrets의 DB_URL/ID/비밀번호로 설정

> [!WARNING]
> 운영 DB는 반드시 퍼블릭 인바운드 제한·권한 최소화·암호화(TLS) 설정을 확인하세요.

---

## 환경 변수(백엔드)
- DB
  - `DB_USERNAME`, `DB_PASSWORD`
  - `DB_URL` (또는 `spring.datasource.url` 직접 지정)
- 메일
  - `MAIL_HOST`=`smtp.gmail.com`, `MAIL_PORT`=`587`
  - `MAIL_USERNAME`, `MAIL_PASSWORD`(앱 비밀번호), `EMAIL_FROM`
- CORS/프런트
  - `ALLOWED_ORIGINS`= `https://<vercel-domain>`
  - `kiki.frontend.url`= 프런트 URL(구독 취소 링크용)
- OpenAI(선택)
  - `OPENAI_API_KEY`, `OPENAI_MODEL`
- 스케줄
  - `SCRAPING_INTERVAL`(ms) — 운영 30분 권장

> [!IMPORTANT]
> 비밀값은 `.env`나 `application-local.yml`에만 두고, git에 커밋하지 마세요. 이 레포에는 루트 `.gitignore`와 `backend/.gitignore`가 이미 설정되어 있습니다.

---

## 키보드 단축키(데스크톱)
- 입력칸 포커스 자동(페이지 진입 시)
- `Shift+Q/W/E`: 니케/마비노기 모바일/명조 선택 토글
- `Shift+Enter`: 즉시 제출
- 입력칸 포커스가 없어도 전역(페이지) 단축키로 동작

> [!TIP]
> 모바일에서는 단축키 UI가 노출되지 않습니다(데스크톱에서만 안내/칩 노출).

---

## 개발 스크립트
```bash
# 프런트
npm run dev        # 개발 서버
npm run build      # 빌드
npm run start      # 프로덕션 실행

# 백엔드
./gradlew bootRun  # 실행
./gradlew test     # 테스트
```

---

## 문제 해결
- CORS 403: 백엔드 `ALLOWED_ORIGINS` 값 확인 → 프런트 도메인/포트를 정확히 등록
- 메일이 수신되지 않음: Gmail 앱 비밀번호/발신 주소/스팸 필터 확인
- 강제 발송이 너무 많음: `hours` 범위를 축소하고 운영에서는 주의해서 사용

> [!IMPORTANT]
> 이 문서는 빠르게 변할 수 있습니다. 배포/운영 전 최신 커밋의 README와 `application.yml`을 항상 확인하세요.
