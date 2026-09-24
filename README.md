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

> 작성 예정

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
