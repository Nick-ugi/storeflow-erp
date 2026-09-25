# StoreFlow ERP — Frontend

React + TypeScript + Vite 기반 SPA. 화면 29개 (docs/requirements/02-screens.md)

```bash
npm install
npm run dev          # http://localhost:5173 (/api, /actuator → localhost:8080 프록시)
npm run build        # 타입 체크 + 프로덕션 빌드
npm run lint         # oxlint
npm run format       # prettier
```

## 기술

| 구분        | 라이브러리                                                                |
| ----------- | ------------------------------------------------------------------------- |
| UI          | Ant Design 6 (한국어 로케일)                                              |
| 서버 데이터 | TanStack Query 5 — 조회 캐시, 등록 · 변경 후 자동 새로고침                |
| HTTP        | axios — 토큰 첨부, 오류를 `ApiError`(code · message · fieldErrors)로 통일 |
| 로그인 상태 | zustand (localStorage 보관)                                               |
| 라우팅      | React Router 8 — 화면별 코드 분할 (`router/pages.ts`)                     |
| 차트        | Recharts                                                                  |

## 구조

```
src/
├── api/          API 호출 (*Api.ts) — 백엔드 호출은 여기에만
├── components/   common(검색 영역 · 선택 목록 · 상품 선택 팝업 등), layout, table
├── hooks/        useSearchState(검색 조건 ↔ URL), useApiError, useLoginUser
├── pages/        화면 (SCR-* 화면 ID 기준)
├── router/       경로 · 역할 가드 · 화면별 코드 분할
├── stores/       zustand 로그인 상태
├── types/        API 타입 (docs/api 명세와 같은 필드명), 코드 값
└── utils/        표시 형식, 코드 → 한글 이름
```

- import 경로는 `@/` alias 사용 (`@/api/productApi` → `src/api/productApi`)
- 역할에 따라 메뉴 · 버튼을 숨기지만, 실제 권한 검사는 서버가 한다. (BR-003)
