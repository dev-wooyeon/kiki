# KIKI — 게임 업데이트 요약 메일러

플레이어가 공지를 찾아다니지 않아도, KIKI가 주기적으로 변화(공지/뉴스)를 감지하고 아침에 한 통의 메일로 핵심만 전달합니다.

> [!NOTE]
> 이 저장소는 단일 레포(monorepo)입니다. `frontend/`는 Next.js, `backend/`는 Kotlin/Spring Boot입니다.

## 무엇을 해결하나요?
- 각 게임 사이트를 매번 방문해야 하는 번거로움
- 공지 범람으로 인해 “핵심만” 파악하기 어려움
- 알림을 통합 관리하고 싶지만 게임사별 채널이 제각각인 문제

## 핵심 경험
- 자동 수집 → 요약 → 아침 한 통 메일로 전달(게임별 그룹화, 링크 모음)
- 데스크톱에서 키보드만으로 빠른 구독 흐름(Shift+Q/W/E, Shift+Enter)
- 라이트/다크 테마 전환, 글래스모피즘 기반 UI

> [!TIP]
> 운영 슬로건은 특정 게임명을 언급하지 않고 “실시간 감지 → 요약 메일” 가치에 집중하는 편이 전환율에 유리합니다.

---

## 실행/배포(요약)
- 프런트: Vercel(프로젝트 루트= `frontend/`)
- 백엔드: Fly.io(Dockerfile/`fly.toml` 제공), DB는 PlanetScale(서버리스 MySQL) 권장
- 상세한 개발/운영 매뉴얼은 각 디렉터리의 README를 참고하세요.

> [!IMPORTANT]
> 스크린샷은 `docs/` 폴더에 자유롭게 추가하시고, 본 문서에 링크 형태로 첨부해 주세요.

---

## 레포 구성
```
.
├─ frontend/        # Next.js 앱 (랜딩/구독/테마/키보드 UX)
├─ backend/         # Spring Boot 앱 (스크래핑/스케줄/메일/모니터링)
├─ .github/workflows
│  ├─ frontend-ci.yml
│  └─ backend-ci.yml
└─ README.md        # 서비스 개요(본 문서)
```

## 링크
- 프런트 가이드: `frontend/README.md`
- 백엔드 가이드: `backend/README.md`

> [!CAUTION]
> 비밀 값은 `.env`나 `application-local.yml` 등 로컬 전용 파일에만 두고, git에 커밋하지 마세요. 루트/백엔드 `.gitignore`가 이미 설정되어 있습니다.

