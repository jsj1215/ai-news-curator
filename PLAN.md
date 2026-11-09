# AI News Curator - 구현 계획

## 현재 완료 상태
- Day 1: Spring Boot 프로젝트 세팅, Entity, JWT Auth
- Day 2-5: 백엔드 파이프라인 (Collector → Filter → Evaluator → Selector → Summarizer → Telegram)
- Day 8-9: 프론트엔드 (Thymeleaf 웹 대시보드 - 5개 페이지 + CSS/JS)
- Day 6-7: 테스트 코드 (Unit + Integration + Collector 총 14개 테스트 클래스)
- Day 10: Docker & 배포 (Dockerfile, docker-compose, prod 설정, CI/CD)
- Day 11: README & 문서화

---

## Day 6-7: 테스트 코드

### Unit Tests
- [x] `RuleBasedFilter` - 키워드 필터링, 제외 키워드, 나이 제한 등
- [x] `TopArticleSelector` - 정렬 및 Top N 선택 로직
- [x] `GeminiEvaluator` - JSON 파싱, 기본 점수 할당, 점수 계산
- [x] `MessageFormatter` - 텔레그램 메시지 포맷 검증
- [x] `CollectorOrchestrator` - 중복 제거 로직

### Integration Tests
- [x] `ArticleRepository` - JPA 쿼리 메서드 검증
- [x] `AuthController` - 회원가입/로그인/토큰 갱신 흐름
- [x] `ArticleController` - 오늘의 기사, 히스토리 조회
- [x] `PipelineController` - 파이프라인 수동 실행

### Collector Tests (Mock 외부 API)
- [x] `HackerNewsCollector` - API 응답 파싱
- [x] `RedditCollector` - RSS 파싱
- [x] `TechBlogCollector` - RSS 파싱, HTML 파싱

---

## Day 8-9: 프론트엔드 (웹 대시보드)

> 기술 스택: Thymeleaf (서버 사이드 렌더링, 별도 빌드 불필요)

### 페이지 구성
- [x] **메인 대시보드** (`/`) - 오늘의 큐레이팅 기사 목록, 점수 표시
- [x] **히스토리** (`/history`) - 날짜별 기사 조회, 달력 네비게이션
- [x] **기사 상세** (`/articles/{id}`) - AI 요약, 점수 상세, 원문 링크
- [x] **파이프라인 관리** (`/admin/pipeline`) - 수동 실행, 상태 확인, 최근 실행 로그
- [x] **통계** (`/admin/stats`) - 소스별 수집량, 평균 점수, API 사용량

### 공통
- [x] 레이아웃 템플릿 (헤더, 네비게이션, 푸터)
- [x] 반응형 CSS (모바일 대응)
- [x] 에러 페이지 (404, 500)

---

## Day 10: Docker & 배포

### Docker 구성
- [x] `Dockerfile` - 멀티스테이지 빌드 (Gradle build → JRE runtime)
- [x] `docker-compose.yml` - 앱 + PostgreSQL (운영 DB 전환)
- [x] `.env.example` - 환경변수 템플릿 (GEMINI_API_KEY, TELEGRAM_BOT_TOKEN 등)

### 배포 준비
- [x] `application-prod.yml` - 운영 프로파일 (PostgreSQL, 로그 설정)
- [x] PostgreSQL 의존성 추가 및 DDL 스크립트
- [x] Health check 엔드포인트 (`/actuator/health`)
- [x] GitHub Actions CI/CD (빌드 → 테스트 → Docker 이미지)

---

## Day 11: README & 문서화

### README.md
- [x] 프로젝트 소개 및 아키텍처 다이어그램
- [x] 기술 스택 정리
- [x] 로컬 실행 가이드 (환경변수, Gradle, Docker)
- [x] API 명세 요약
- [ ] 스크린샷 (대시보드, 텔레그램 알림)

### 기타
- [ ] API 문서 (Swagger/SpringDoc 연동)
- [ ] CLAUDE.md (프로젝트 컨텍스트)

---

## 우선순위 요약

| 순서 | 작업 | 일정 | 이유 |
|------|------|------|------|
| 1 | 테스트 코드 | Day 6-7 | 이후 리팩토링/기능 추가의 안전망 |
| 2 | 프론트엔드 | Day 8-9 | 텔레그램 외 웹으로 기사 확인 가능 |
| 3 | Docker & 배포 | Day 10 | 실제 서버에서 자동 운영 |
| 4 | README & 문서화 | Day 11 | 포트폴리오 및 유지보수용 |
