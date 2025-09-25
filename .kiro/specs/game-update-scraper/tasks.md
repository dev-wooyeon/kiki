# Implementation Plan

- [x] 1. 백엔드 프로젝트 초기 설정 및 기본 구조 생성

  - Kotlin + Spring Boot 프로젝트 생성 (Gradle 기반)
  - 필수 의존성 추가 (Web, JPA, Mail, Validation, MySQL, Jsoup)
  - 기본 패키지 구조 생성 (controller, service, repository, entity, config)
  - application.yml 기본 설정 파일 작성
  - _Requirements: 6.3, 6.4_

- [x] 2. 데이터베이스 모델 및 JPA 엔티티 구현

  - [x] 2.1 JPA 엔티티 클래스 작성

    - Game, GameNotice, Subscriber, EmailLog 엔티티 구현
    - 엔티티 간 관계 매핑 (@OneToMany, @ManyToOne) 설정
    - 인덱스 및 제약조건 어노테이션 추가
    - _Requirements: 1.3, 1.4, 2.2_

  - [x] 2.2 JPA Repository 인터페이스 구현
    - 각 엔티티별 Repository 인터페이스 작성
    - 커스텀 쿼리 메서드 정의 (findByIsActiveTrue, findByPublishedDateAfter 등)
    - Repository 단위 테스트 작성
    - _Requirements: 1.4, 3.1, 5.1_

- [x] 3. 게임별 스크래핑 모듈 구현

  - [x] 3.1 스크래핑 인터페이스 및 공통 로직 구현

    - GameScraper 인터페이스 정의
    - 공통 HTTP 클라이언트 및 파싱 유틸리티 구현
    - 오류 처리 및 재시도 로직 구현 (지수 백오프)
    - _Requirements: 1.1, 1.5, 6.1, 6.4_

  - [x] 3.2 니케(NIKKE) 스크래퍼 구현

    - 니케 공식 사이트 공지사항 페이지 구조 분석
    - NikkeScraper 클래스 구현 (제목, URL, 게시일자, 내용 추출)
    - 스크래퍼 단위 테스트 작성 (Mock HTML 응답 활용)
    - _Requirements: 1.2, 1.3_

  - [x] 3.3 마비노기 모바일 스크래퍼 구현

    - 마비노기 모바일 공지사항 페이지 구조 분석
    - MabinogiMobileScraper 클래스 구현
    - 스크래퍼 단위 테스트 작성
    - _Requirements: 1.2, 1.3_

  - [x] 3.4 명조(Genshin) 스크래퍼 구현
    - 명조 공식 사이트 공지사항 페이지 구조 분석
    - GenshinScraper 클래스 구현
    - 스크래퍼 단위 테스트 작성
    - _Requirements: 1.2, 1.3_

- [x] 4. 스크래핑 서비스 및 스케줄링 구현

  - [x] 4.1 스크래핑 서비스 구현

    - ScrapingService 클래스 구현 (모든 게임 스크래퍼 통합 관리)
    - 중복 공지사항 필터링 로직 구현
    - 새로운 공지사항만 데이터베이스 저장 로직 구현
    - _Requirements: 1.2, 1.4_

  - [x] 4.2 스케줄링 서비스 구현
    - @Scheduled 어노테이션을 활용한 30분 주기 스케줄링 구현
    - 스케줄링 작업 실행 로그 기록 기능 구현
    - 스케줄링 서비스 통합 테스트 작성
    - _Requirements: 1.1, 6.4_

- [x] 5. 구독 관리 API 구현

  - [x] 5.1 구독 서비스 구현

    - SubscriptionService 클래스 구현
    - 이메일 중복 검증 및 구독 등록 로직 구현
    - 구독취소 토큰 생성 및 관리 로직 구현
    - _Requirements: 2.2, 2.4, 4.2, 4.3_

  - [x] 5.2 구독 관리 REST API 구현
    - POST /api/subscribe 엔드포인트 구현 (이메일 구독 등록)
    - GET /api/unsubscribe/{token} 엔드포인트 구현 (구독 취소)
    - 입력 검증 및 오류 응답 처리 구현
    - API 통합 테스트 작성
    - _Requirements: 2.1, 2.3, 2.5, 4.1, 4.4_

- [x] 6. 이메일 발송 시스템 구현

  - [x] 6.1 이메일 템플릿 및 서비스 구현

    - HTML 이메일 템플릿 작성 (게임별 공지사항 그룹화)
    - EmailService 클래스 구현 (Spring Mail 활용)
    - 이메일 발송 실패 처리 및 로그 기록 구현
    - _Requirements: 3.2, 3.3, 3.4, 6.2_

  - [x] 6.2 이메일 발송 스케줄링 통합
    - 새로운 공지사항 감지 시 이메일 발송 트리거 구현
    - 구독자 목록 조회 및 일괄 이메일 발송 로직 구현
    - 이메일 발송 서비스 통합 테스트 작성
    - _Requirements: 3.1, 3.5_

- [ ] 7. 프론트엔드 프로젝트 초기 설정

  - Next.js 프로젝트 생성 및 기본 설정
  - 필수 패키지 설치 (Tailwind CSS, Framer Motion, React Hook Form, Axios)
  - 기본 레이아웃 및 라우팅 구조 설정
  - 환경변수 설정 (API 엔드포인트)
  - _Requirements: 2.1, 5.1_

- [ ] 8. 랜딩 페이지 구현

  - [ ] 8.1 구독 폼 컴포넌트 구현

    - 이메일 입력 폼 및 유효성 검증 구현
    - 구독 API 호출 및 응답 처리 구현
    - 로딩 상태 및 오류 메시지 표시 구현
    - _Requirements: 2.1, 2.3, 2.4, 2.5_

  - [ ] 8.2 랜딩 페이지 UI 구현
    - Glassmorphism 스타일 적용한 히어로 섹션 구현
    - 서비스 소개 및 기능 카드 컴포넌트 구현
    - 반응형 디자인 및 애니메이션 효과 적용
    - _Requirements: 2.1_

- [ ] 9. 구독 관리 페이지 구현

  - [ ] 9.1 구독 성공 페이지 구현

    - 구독 완료 확인 메시지 및 안내 페이지 구현
    - 구독 성공 후 리다이렉트 처리 구현
    - _Requirements: 2.3_

  - [ ] 9.2 구독 취소 페이지 구현
    - 구독 취소 확인 페이지 구현
    - 구독 취소 API 호출 및 결과 표시 구현
    - _Requirements: 4.3, 4.4_

- [ ] 10. 공지사항 아카이브 페이지 구현

  - [ ] 10.1 공지사항 목록 API 구현

    - GET /api/notices 엔드포인트 구현 (최근 30일 공지사항 조회)
    - 날짜별, 게임별 필터링 쿼리 파라미터 지원
    - 페이징 처리 구현
    - _Requirements: 5.1, 5.2_

  - [ ] 10.2 아카이브 페이지 UI 구현
    - Soft UI 스타일 적용한 공지사항 카드 컴포넌트 구현
    - 게임별, 날짜별 필터링 UI 구현
    - 공지사항 클릭 시 원본 사이트 새 창 이동 구현
    - _Requirements: 5.2, 5.3, 5.4, 5.5_

- [ ] 11. 시스템 통합 테스트 및 배포 준비

  - [ ] 11.1 백엔드 통합 테스트 구현

    - 전체 스크래핑 파이프라인 E2E 테스트 작성
    - 이메일 발송 플로우 통합 테스트 작성
    - 데이터베이스 트랜잭션 테스트 작성
    - _Requirements: 1.1, 1.4, 3.1, 6.1_

  - [ ] 11.2 프론트엔드 통합 테스트 구현
    - 구독 플로우 E2E 테스트 작성
    - API 통신 및 오류 처리 테스트 작성
    - 반응형 디자인 테스트 작성
    - _Requirements: 2.1, 2.3, 5.1_

- [ ] 12. 배포 환경 설정 및 배포

  - [ ] 12.1 백엔드 AWS EC2 배포

    - EC2 인스턴스 생성 및 환경 설정
    - MySQL 데이터베이스 설정 (RDS 또는 로컬)
    - Spring Boot 애플리케이션 배포 및 실행
    - 환경변수 및 보안 설정 적용
    - _Requirements: 6.1, 6.2, 6.4_

  - [ ] 12.2 프론트엔드 Vercel 배포
    - GitHub 저장소 연동 및 자동 배포 설정
    - 환경변수 설정 (API 엔드포인트)
    - 도메인 연결 및 HTTPS 설정
    - _Requirements: 2.1, 5.1_

- [ ] 13. 운영 모니터링 및 최종 검증

  - [ ] 13.1 로깅 및 모니터링 설정

    - 애플리케이션 로그 설정 및 로그 레벨 조정
    - 스크래핑 작업 성공/실패 모니터링 구현
    - 이메일 발송 통계 및 오류 추적 구현
    - _Requirements: 6.4, 6.2_

  - [ ] 13.2 최종 시스템 검증
    - 실제 게임 사이트 대상 스크래핑 테스트 수행
    - 이메일 발송 기능 실제 환경 테스트
    - 전체 사용자 플로우 검증 (구독 → 이메일 수신 → 구독 취소)
    - 성능 및 안정성 최종 확인
    - _Requirements: 1.1, 1.2, 2.1, 3.1, 4.1, 5.1_
