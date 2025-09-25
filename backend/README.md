# Backend — KIKI Spring Boot

> [!NOTE]
> Kotlin/Spring Boot 3 기반 백엔드로, 스크래핑/스케줄링/요약 메일/모니터링을 담당합니다.

## 아키텍처 개요
```
Scrapers (Nikke, Mabinogi, Genshin, WutheringWaves)
   │  (Jsoup/JSON)
   ▼
ScrapingService ─── dedup/save(GameNotice)
   │                       │
   │                   JPA/Hibernate (MySQL/H2)
   ▼
SchedulingService (@Scheduled fixedRate)
   │
   ├─ when new → EmailService.sendDailyDigest()
   └─ MonitoringController (manual scrape/notify, health)
```

> [!IMPORTANT]
> 운영 프로필은 기본(`application.yml`)을 사용하고, 로컬 테스트는 `application-local.yml`로 H2/로컬 메일 설정을 사용합니다.

## 패키지 구성
```
com.kiki
├─ config/              # CORS/게임 Seed/KikiProperties
├─ controller/          # SubscriptionController, MonitoringController
├─ dto/                 # ApiResponse, SubscribeRequest 등
├─ entity/              # Game, GameNotice, Subscriber, EmailLog
├─ repository/          # GameRepository, GameNoticeRepository 등
├─ scraper/
│  ├─ AbstractGameScraper, GameScraper
│  ├─ NikkeScraper, MabinogiMobileScraper, GenshinScraper, WutheringWavesScraper
│  ├─ client/ (예: WutheringWavesClient)
│  └─ util (HttpClientUtil, ParsingUtil)
├─ service/
│  ├─ ScrapingService     # 스크랩/중복제거/저장/조회
│  ├─ SchedulingService    # 고정 주기 실행 + 모니터링 메트릭
│  ├─ EmailService         # 메일 작성/발송(HTML 템플릿 내장)
│  ├─ NoticeSummaryService # 요약/AI 요약(선택적)
│  └─ NoticeContentService # 본문 추출(요약 품질 향상용)
└─ integration/
   └─ OpenAiClient        # Chat Completions 호출 (옵션)
```

## 주요 기능
- 스크래핑: 사이트 구조/JSON API 기반 파싱, URL 중복 방지
- 스케줄: 기본 30분 고정 간격(@Scheduled)
- 메일 발송: 게임별 그룹화/링크 모음/상단 빠른 요약(옵션)
- 구독/취소/통계: 이메일 기반 구독, 토큰으로 취소, 활성 구독자 수 제공
- 모니터링: health/schedule, 수동 스크래핑, 강제 발송(force)

## 실행 방법
### 로컬(H2 + Gmail 앱 비밀번호)
```bash
cd backend
./gradlew bootRun -Dspring.profiles.active=local
```
- `src/main/resources/application-local.yml`에서 H2, 메일 자격 증명을 설정하세요.
- H2 콘솔: `/h2-console` (local에서만 enable)

### 운영(MySQL)
- `application.yml`의 데이터소스(MySQL) 사용
- Fly.io 배포 템플릿: `backend/Dockerfile`, `backend/fly.toml`

> [!WARNING]
> DB, 메일 계정, OpenAI 키 등 비밀값은 Fly Secrets/환경변수로 주입하세요. Git에 커밋하지 마세요.

## 환경 변수(요약)
- DB: `DB_USERNAME`, `DB_PASSWORD`, `DB_URL`
- CORS: `ALLOWED_ORIGINS`
- 메일: `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `EMAIL_FROM`
- OpenAI(옵션): `OPENAI_API_KEY`, `OPENAI_MODEL`
- 기타: `SCRAPING_INTERVAL`(ms), `FRONTEND_URL`

## 엔드포인트
- 구독: `POST /api/subscribe` { email }
- 구독 취소: `GET /api/unsubscribe/{token}`
- 통계: `GET /api/subscribe/stats` → { activeCount }
- 모니터링:
  - `GET /api/monitoring/health`
  - `GET /api/monitoring/schedule`
  - `POST /api/monitoring/scrape`
  - `POST /api/monitoring/notifications?force=true&hours=24`

> [!CAUTION]
> 강제 발송(force)은 모니터링/QA 용도입니다. 운영에서는 멱등성/율제어, 메일 발송 제한 정책을 고려하세요.

## 테스트
```bash
./gradlew test
```
- 주요 서비스/스크래퍼/이메일 동작에 대한 단위/통합 테스트를 포함합니다.

