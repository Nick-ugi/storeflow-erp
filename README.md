# StoreFlow ERP

판매·재고·발주 통합 관리 시스템 — *Legacy ERP → Modern Web Architecture*

소규모 매장의 상품, 판매, 재고, 발주 및 입고 업무를 통합 관리하는 Web ERP 시스템

## 1. Project Overview

> 작성 예정

## 2. Background

> 작성 예정 — AS400 / RPG / DB2 기반 레거시 업무 시스템 분석 경험을 Web 아키텍처로 재설계

## 3. Goals

> 작성 예정

## 4. Main Features

> 작성 예정 — 회원/권한, 상품, 판매, 재고, 발주, Dashboard

## 5. System Architecture

> 작성 예정

## 6. Tech Stack

| 구분 | 기술 |
|---|---|
| Frontend | React 19, TypeScript 6, Vite 8 |
| Backend | Java 21, Spring Boot 4.0, Spring Security, MyBatis |
| Database | PostgreSQL 17, Flyway |
| Infra | Docker, GitHub Actions |

## 7. ERD

> 작성 예정 — [docs/erd](docs/erd)

## 8. Business Flow

> 작성 예정 — [docs/flow](docs/flow)

## 9. API Documentation

> 작성 예정 — [docs/api](docs/api)

## 10. Authentication

> 작성 예정

## 11. Transaction Processing

> 작성 예정

## 12. Docker

> 작성 예정

## 13. CI/CD

> 작성 예정

## 14. Deployment

> 작성 예정

## 15. Troubleshooting

### 비활성화한 사용자의 토큰이 계속 통과함 — MyBatis 1차 캐시

- **문제**: 사용자를 비활성화한 뒤에도 같은 트랜잭션 안의 다음 요청에서 인증이 통과함 (통합 테스트에서 발견)
- **원인**: MyBatis 기본 설정(`localCacheScope=SESSION`)은 같은 SqlSession 안에서 동일한 조회 결과를 캐시한다. JDBC로 바뀐 사용자 상태를 MyBatis가 다시 읽지 않고 캐시 값을 돌려줌
- **해결**: `mybatis.configuration.local-cache-scope: statement` — 조회마다 DB를 다시 읽도록 변경
- **결과**: 인증뿐 아니라 한 트랜잭션에서 재고를 다시 읽는 판매 · 입고 처리에서도 오래된 값을 읽을 위험을 제거

### 롤백 테스트가 항상 실패함 — 테스트 트랜잭션과 서비스 트랜잭션

- **문제**: "재고 부족 시 판매 전체 롤백" 테스트에서 실패한 판매 데이터가 조회됨
- **원인**: `@Transactional` 테스트 안에서는 서비스 트랜잭션이 테스트 트랜잭션에 참여한다. 예외가 나도 롤백 표시만 되고 실제 롤백은 테스트가 끝날 때 일어나므로, 같은 트랜잭션에서는 저장된 행이 보임
- **해결**: 실제 커밋 · 롤백이 필요한 테스트(롤백, 동시성)는 테스트 트랜잭션 없이 실행하는 `RealTransactionTestSupport`로 분리하고, 테스트 후 데이터를 정리
- **결과**: 롤백 · 동시성 규칙을 실제 트랜잭션으로 검증. 잠금(`FOR UPDATE`)이나 조건부 취소를 일부러 제거하면 해당 테스트가 실패하는 것도 확인 (뮤테이션 테스트)

## 16. Lessons Learned

> 작성 예정

---

## Getting Started (로컬 개발 환경)

**필요 도구:** JDK 21, Node.js 20.19+ (또는 22.12+), Docker Desktop

```bash
# 1. DB 실행 (PostgreSQL 17)
docker compose up -d

# 2. Backend 실행 → http://localhost:8080
cd backend
./gradlew bootRun

# 3. Frontend 실행 → http://localhost:5173
cd frontend
npm install
npm run dev
```

브라우저에서 http://localhost:5173 접속 시 `Backend + DB: UP`이 표시되면 환경 구성 완료.
DB 접속 정보는 [.env.example](.env.example) 참고.

- **초기 ADMIN 계정**: 아이디 `admin` / 비밀번호 `admin1234` (첫 실행 시 Flyway가 생성, 로그인 후 변경 권장)
- **테스트**: `cd backend && ./gradlew test` — Testcontainers가 테스트용 PostgreSQL 컨테이너를 따로 띄우므로 Docker가 실행 중이어야 한다.
- **운영 실행 시**: `JWT_SECRET` 환경변수(32자 이상)를 반드시 지정한다. 로컬 프로필(`local`)에서만 개발용 키를 사용한다.
