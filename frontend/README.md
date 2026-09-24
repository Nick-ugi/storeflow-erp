# StoreFlow ERP — Frontend

React + TypeScript + Vite 기반 SPA.

```bash
npm install
npm run dev          # http://localhost:5173 (/api, /actuator → localhost:8080 프록시)
npm run build        # 타입 체크 + 프로덕션 빌드
npm run lint         # oxlint
npm run format       # prettier
```

- import 경로는 `@/` alias 사용 (`@/api/productApi` → `src/api/productApi`)
