# 구동 흐름 (개발자용)

코드가 **어떤 순서로, 어떤 클래스를 거쳐** 실행되는지 정리한 문서다.
업무 규칙(왜 그렇게 하는가)은 [상세 기능 명세](../requirements/), 요청 · 응답 형식은 [API 명세](../api/)를 본다.

- 표기: `클래스.메서드` — 백엔드는 `backend/src/main/java/com/storeflow/` 아래, 프론트엔드는 `frontend/src/` 아래 기준
- ★ 표시는 재고가 바뀌는 트랜잭션

| 장 | 내용 |
|---|---|
| 1 | [전체 구성](#1-전체-구성) |
| 2 | [백엔드 기동](#2-백엔드-기동) |
| 3 | [화면 진입 (프론트엔드 라우팅)](#3-화면-진입-프론트엔드-라우팅) |
| 4 | [API 요청 한 건의 처리](#4-api-요청-한-건의-처리) |
| 5 | [오류 처리](#5-오류-처리) |
| 6 | [인증 — 로그인 · 토큰 · 만료 · 로그아웃](#6-인증--로그인--토큰--만료--로그아웃) |
| 7 | [재고 변경 공통 절차 ★](#7-재고-변경-공통-절차-) |
| 8 | [판매 등록 ★](#8-판매-등록-) |
| 9 | [판매 취소 ★](#9-판매-취소-) |
| 10 | [발주 상태 전이와 입고 ★](#10-발주-상태-전이와-입고-) |
| 11 | [매장 · 상품 등록과 재고 행 생성](#11-매장--상품-등록과-재고-행-생성) |
| 12 | [트랜잭션 · 잠금 요약](#12-트랜잭션--잠금-요약) |
| 13 | [빌드 · 배포](#13-빌드--배포) |

---

## 1. 전체 구성

```mermaid
flowchart LR
    subgraph dev["로컬 개발"]
        direction LR
        b1["브라우저"] --> vite["Vite 개발 서버 :5173<br/>React 화면 + HMR"]
        vite -- "/api/*, /actuator/* 프록시" --> be1["Spring Boot :8080<br/>local 프로필"]
        be1 --> db1[("PostgreSQL :5432<br/>루트 .env의 DB")]
    end
    subgraph prod["Docker 배포 (docker/docker-compose.yml)"]
        direction LR
        b2["브라우저"] --> nginx["Nginx :80<br/>빌드된 화면 제공"]
        nginx -- "/api/* 만 전달" --> be2["Spring Boot :8080<br/>prod 프로필"]
        be2 --> db2[("PostgreSQL<br/>볼륨 db-data")]
    end
```

| 계층 | 위치 | 하는 일 |
|---|---|---|
| 화면 | `pages/*` | 입력 · 표시. 역할에 따라 메뉴 · 버튼을 숨김 (권한 판단은 서버) |
| 서버 데이터 | TanStack Query (`useQuery` · `useMutation`) | 조회 캐시, 등록 · 변경 후 `invalidateQueries()`로 전체 새로고침 |
| API 호출 | `api/*Api.ts` → `api/client.ts` | axios. 토큰 첨부, 오류를 `ApiError`로 변환 |
| 보안 | `common/config/SecurityConfig` | JWT 검증, 요청마다 사용자를 DB에서 다시 조회 |
| Controller | `{도메인}/controller` | URL, 역할 제한(`@PreAuthorize`), 입력 검증(`@Valid`) |
| Service | `{도메인}/service` | 업무 규칙, 트랜잭션(`@Transactional`), 매장 범위(`LoginUser`) |
| Mapper | `{도메인}/mapper` + `resources/mapper/*.xml` | SQL. `snake_case` ↔ `camelCase`, 일시는 KST(`+09:00`)로 변환 |
| DB | PostgreSQL 17 | 제약조건(마지막 방어선), 문서번호 시퀀스, 행 잠금 |

## 2. 백엔드 기동

```mermaid
flowchart TD
    start(["StoreFlowApplication.main()"]) --> profile{"SPRING_PROFILES_ACTIVE<br/>지정?"}
    profile -- "없음 (로컬 실행)" --> local["local 프로필<br/>application.yml + application-local.yml<br/>+ 루트 .env (spring.config.import, 없으면 건너뜀)"]
    profile -- "prod (Docker 이미지)" --> prodp["prod 프로필<br/>application.yml + 환경변수"]
    local --> key
    prodp --> key{"JWT 키가 있고<br/>32바이트 이상?"}
    key -- 아니오 --> fail1(["기동 실패<br/>(prod에서 JWT_SECRET 누락 포함)"])
    key -- 예 --> flyway{"DB 연결 (HikariCP)<br/>Flyway db/migration 확인"}
    flyway -- "새 버전 있음" --> migrate["V1 스키마 · V2 초기 ADMIN 적용<br/>flyway_schema_history에 기록"]
    flyway -- "적용된 파일이 수정됨" --> fail2(["기동 실패<br/>체크섬 불일치"])
    flyway -- "변경 없음" --> ready
    migrate --> ready(["MyBatis 준비 (mapper/*.xml · KST 타입 핸들러)<br/>Tomcat :8080 요청 대기 · /actuator/health = UP<br/>Docker: healthcheck 통과 후 Nginx 기동"])
```

- 설정 값 우선순위: 환경변수 > 루트 `.env` > `application.yml` 기본값. (`DB_HOST` · `DB_PORT` · `DB_NAME` · `DB_USERNAME` · `DB_PASSWORD` · `JWT_SECRET`)
- 날짜 계산은 모두 주입받은 `Clock`(`ClockConfig`, Asia/Seoul) 기준이다. 서버 · DB의 시간대 설정과 관계없이 "오늘"이 한국 날짜로 계산된다.
- `local-cache-scope: statement` — 같은 트랜잭션 안에서도 조회할 때마다 DB를 다시 읽는다. 잠금 후 최신 수량을 읽어야 하는 재고 처리의 전제 조건이다.

## 3. 화면 진입 (프론트엔드 라우팅)

```mermaid
flowchart TD
    url(["주소 입력 · 메뉴 클릭 · 새로고침"]) --> router["RouterProvider<br/>router/index.tsx"]
    router --> isLogin{"/login ?"}
    isLogin -- 예 --> login["LoginPage"]
    isLogin -- 아니오 --> auth{"RequireAuth<br/>토큰 있음?"}
    auth -- "없음 · 직접 로그아웃한 경우" --> login
    auth -- "없음 · 그 밖의 경우" --> loginR["/login?redirect=원래 주소<br/>(로그인 후 돌아옴)"]
    auth -- 있음 --> layout["AppLayout<br/>역할별 메뉴 · GET /auth/me로 사용자 정보 갱신"]
    layout --> role{"RequireRole<br/>발주 · 상품 등록/수정 · 매장 · 사용자"}
    role -- 역할 없음 --> e403["/403 ErrorPage"]
    role -- "통과 · 제한 없는 화면" --> page["화면 컴포넌트<br/>router/pages.ts에서 지연 로딩 (로딩 중 Skeleton)<br/>useSearchState: 검색 조건 · 페이지 ↔ URL"]
    page --> query["useQuery → api/*Api.ts (4장)"]
    query -- 성공 --> render["표 · 상세 렌더링"]
    query -- "상세 조회 403 · 404" --> qerr["QueryError → ErrorPage"]
```

- 로그인 상태는 `stores/authStore.ts`(zustand)가 localStorage `storeflow-auth`에 보관한다. 새로고침해도 유지된다.
- 검색 조건이 URL에 있으므로 새로고침 · 뒤로 가기 · 주소 공유 시 같은 목록이 다시 보인다.

## 4. API 요청 한 건의 처리

판매 내역 조회(`GET /api/v1/sales`)를 예로 든 정상 흐름이다. 다른 API도 같은 경로를 지난다.

```mermaid
sequenceDiagram
    autonumber
    participant P as 화면 (useQuery)
    participant C as api/client.ts
    participant X as Vite 프록시 / Nginx
    participant F as Security 필터
    participant K as Controller
    participant S as Service
    participant M as Mapper (XML)
    participant D as PostgreSQL

    P->>C: salesApi.search(검색 조건)
    C->>C: 빈 값 파라미터 제거, Authorization: Bearer 토큰
    C->>X: GET /api/v1/sales?...
    X->>F: 그대로 전달
    F->>F: JwtDecoder — HS256 서명 · 만료 · iss=storeflow 검증
    F->>D: LoginUserAuthenticationConverter → AuthMapper.findById(토큰의 사용자 ID)
    D-->>F: 사용자 (상태 · 역할 · 소속 매장)
    F->>K: SecurityContext = LoginUser + ROLE_역할
    K->>K: @PreAuthorize 역할 검사, @Valid 입력 검증
    K->>S: search(loginUser, 조건)
    S->>S: loginUser.scopeStoreId() — MANAGER · USER는 소속 매장으로 고정
    S->>M: findSales · countSales
    M->>D: 동적 SQL (조건 · 페이징)
    D-->>M: 행
    M-->>S: DTO (snake_case → camelCase, 일시 +09:00)
    S-->>K: PageResponse
    K-->>C: 200 · success true · data
    C-->>P: response.data.data 만 꺼내 반환
    P->>P: queryKey로 캐시 저장 → 렌더링
```

**매장 범위** — 컨트롤러는 요청의 `storeId`를 그대로 넘기고, 서비스가 `LoginUser`로 범위를 정한다.

| 메서드 | 쓰는 곳 | ADMIN | MANAGER · USER |
|---|---|---|---|
| `scopeStoreId(요청 storeId)` | 목록 · 집계 | 요청한 매장 (없으면 전 매장) | 항상 소속 매장 (요청 무시) |
| `requireStoreId(요청 storeId, 메시지)` | 등록 · 단일 매장 처리 | 필수 (없으면 400) | 항상 소속 매장 |
| `checkStoreAccess(문서의 storeId)` | 판매 · 발주 상세 · 처리 | 통과 | 다른 매장이면 403 |

## 5. 오류 처리

```mermaid
flowchart TD
    subgraph server["백엔드"]
        e1["토큰 없음 · 위조 · 만료<br/>사용자 없음 · 비활성"] --> seh["SecurityErrorHandler"]
        e2["@PreAuthorize 역할 불일치"] --> seh
        seh --> geh["GlobalExceptionHandler"]
        e3["@Valid 실패 · JSON 형식 오류"] --> geh
        e4["BusinessException(ErrorCode)<br/>업무 규칙 위반"] --> geh
        e5["DuplicateKeyException<br/>UNIQUE 제약 위반 → 제약 이름으로 DUPLICATE_* 결정"] --> geh
        e6["예상하지 못한 예외<br/>(서버 로그에 스택 기록)"] --> geh
        geh --> body["HTTP 상태 + success false<br/>error: code · message · fieldErrors"]
    end
    body --> intc["client.ts 응답 인터셉터<br/>ApiError(status, code, message, fieldErrors)"]
    subgraph client["프론트엔드"]
        intc --> unauth{"code = UNAUTHORIZED<br/>이고 토큰 있음?"}
        unauth -- 예 --> relogin["logout() → /login?expired=1&redirect=현재 주소"]
        unauth -- 아니오 --> kind{"어디서 난 오류?"}
        kind -- "등록 · 변경 (useMutation)" --> ue["useApiError<br/>message.error + 폼 항목에 fieldErrors 표시"]
        kind -- "상세 조회 403 · 404" --> qe["QueryError → ErrorPage"]
    end
```

| 원인 | code | HTTP |
|---|---|---|
| 토큰 없음 · 위조 · 만료, 사용자 없음 · 비활성 | `UNAUTHORIZED` | 401 |
| 역할 없음, 다른 매장의 문서 | `FORBIDDEN` | 403 |
| 입력 검증 실패 (항목별 `fieldErrors` 포함) | `VALIDATION_ERROR` | 400 |
| 로그인 실패 — 코드가 `UNAUTHORIZED`가 아니므로 인터셉터가 로그아웃시키지 않음 | `LOGIN_FAILED` | 401 |
| 재고 부족 / 이미 취소된 판매 / 발주 상태 불일치 | `INSUFFICIENT_STOCK` / `SALE_ALREADY_CANCELLED` / `INVALID_PURCHASE_ORDER_STATUS` | 409 |
| 매장코드 · 아이디 · 상품코드 등 중복 | `DUPLICATE_*` | 409 |
| 그 밖의 예외 | `INTERNAL_ERROR` | 500 |

- `BusinessException`은 `RuntimeException`이므로 `@Transactional` 메서드 안에서 던지면 그 트랜잭션 전체가 롤백된다.
- 조회(`useQuery`)는 4xx면 재시도하지 않고, 네트워크 · 5xx 오류만 2번까지 재시도한다. (`main.tsx`)

## 6. 인증 — 로그인 · 토큰 · 만료 · 로그아웃

```mermaid
sequenceDiagram
    autonumber
    actor U as 사용자
    participant L as LoginPage / AppLayout
    participant A as authStore (localStorage)
    participant C as api/client.ts
    participant B as AuthService
    participant J as JwtTokenProvider
    participant D as PostgreSQL

    U->>L: 아이디 · 비밀번호 입력
    L->>B: POST /api/v1/auth/login
    B->>D: AuthMapper.findByUsername
    alt 사용자 없음 또는 비밀번호 불일치 (BCrypt)
        B-->>L: 401 LOGIN_FAILED (두 경우 같은 메시지)
    else 비밀번호는 맞지만 비활성 계정
        B-->>L: 403 ACCOUNT_INACTIVE
    else 정상
        B->>J: createToken(사용자 ID)
        J-->>B: HS256 JWT (iss=storeflow, sub=사용자 ID, 8시간)
        B-->>L: accessToken · expiresIn · 사용자 정보
        L->>A: login(token, user)
        L->>L: redirect 주소 또는 /dashboard로 이동
    end

    Note over L,D: 이후 모든 요청 — Bearer 토큰 검증 후 DB에서 사용자를 다시 읽는다 (4장)<br/>비활성화 · 역할 변경 · 매장 변경이 토큰 만료를 기다리지 않고 다음 요청부터 반영

    L->>B: GET /auth/me (AppLayout 진입 시)
    B-->>A: setUser — 메뉴 · 버튼 표시를 최신 역할로 갱신

    opt 토큰 만료 또는 사용자 비활성화
        B-->>C: 401 UNAUTHORIZED
        C->>A: logout()
        C->>L: /login?expired=1&redirect=현재 주소
    end

    opt 사용자가 직접 로그아웃
        L->>B: POST /auth/logout (서버는 상태가 없어 처리할 것 없음)
        L->>A: logout(true) + queryClient.clear()
        Note over L,A: 다음에 다른 계정이 로그인할 수 있으므로 이전 화면(redirect)을 기억하지 않음
    end
```

- 토큰에는 사용자 ID만 담는다. 역할 · 매장은 토큰이 아니라 DB에서 읽으므로 토큰을 위조해 역할을 바꿀 수 없다.
- 서명 키: local은 `application-local.yml`의 개발용 키, prod는 `JWT_SECRET` 환경변수. 서로 다른 키이므로 로컬에서 받은 토큰은 운영 서버에서 401이 된다.

## 7. 재고 변경 공통 절차 ★

재고 수량은 **`StockService.change()` 한 곳에서만** 바뀐다.

```mermaid
flowchart TD
    callers["호출하는 업무 (모두 @Transactional)<br/>SaleService.create · cancel / PurchaseOrderService.receive / StockService.adjust<br/>→ StockService.change(StockChangeCommand)"] --> tx{"MANDATORY<br/>진행 중인 트랜잭션 있음?"}
    tx -- 없음 --> ex0(["IllegalTransactionStateException<br/>단독 호출 차단"])
    tx -- 있음 --> lock["① 변경 목록을 상품 ID 오름차순 정렬<br/>② StockMapper.lockStocks: SELECT … ORDER BY product_id FOR UPDATE OF st<br/>③ 다른 트랜잭션이 잡은 행이면 끝날 때까지 대기 → 최신 수량을 읽음"]
    lock --> row{"상품마다 반복<br/>재고 행 있음?"}
    row -- 없음 --> ex1(["IllegalStateException<br/>재고 행은 등록 시 미리 만들어 둠 (11장)"])
    row -- 있음 --> calc["변동 후 = 변동 전 + 변동 수량<br/>판매 −, 취소 · 입고 +, 조정 ±"]
    calc --> neg{"변동 후가<br/>0 미만?"}
    neg -- 예 --> ex2(["409 INSUFFICIENT_STOCK<br/>호출한 업무 전체 롤백"])
    neg -- 아니오 --> upd["UPDATE stocks SET quantity = 변동 후<br/>INSERT stock_histories (유형 · 변동 · 전 · 후 · 관련 문서 · 사유 · 처리자)"]
    upd --> more{"남은 상품?"}
    more -- 예 --> row
    more -- 아니오 --> done(["결과 반환<br/>호출한 업무가 커밋할 때 문서 · 재고 · 이력이 함께 반영"])
```

| 업무 | 명령 | 이력 유형 | 변동 수량 | 관련 문서 |
|---|---|---|---|---|
| 판매 등록 | `StockChangeCommand.sale` | `SALE` | − 판매 수량 | `SALE`, 판매 ID |
| 판매 취소 | `StockChangeCommand.saleCancel` | `SALE_CANCEL` | + 판매 수량 | `SALE`, 판매 ID |
| 입고 | `StockChangeCommand.purchase` | `PURCHASE` | + 발주 수량 | `PURCHASE_ORDER`, 발주 ID |
| 재고 조정 | `StockChangeCommand.adjustment` | `ADJUSTMENT` | ± 1 ~ 9,999 | 없음 (사유 필수) |

- **MANDATORY**: 이 메서드를 트랜잭션 없이 부르면 예외가 난다. "문서는 저장됐는데 재고는 안 바뀜" 같은 반쪽 처리를 구조적으로 막는다.
- **정렬 후 잠금**: 여러 상품을 다루는 요청들이 항상 같은 순서로 잠그므로 서로를 기다리는 교착 상태가 생기지 않는다.
- **DB 제약조건**: 코드가 잘못되어도 `ck_stocks_quantity`(재고 0 이상), `ck_stock_histories_quantity`(변동 후 = 변동 전 + 변동 수량), `ck_stock_histories_type`(유형별 부호 · 관련 문서)이 잘못된 값을 거부한다.

## 8. 판매 등록 ★

```mermaid
sequenceDiagram
    autonumber
    actor U as 사용자
    participant P as SaleCreatePage
    participant K as Controller
    participant S as SaleService
    participant R as Store/ProductService
    participant N as DocumentNumberGenerator
    participant T as StockService
    participant D as PostgreSQL

    U->>P: 상품 선택 팝업 (ProductSelectModal)
    P->>K: GET /api/v1/products?storeId=…&status=ACTIVE
    K-->>P: 상품 · 판매가 · 해당 매장 재고
    Note over P: 이미 담긴 상품은 새 줄 없이 수량 +1<br/>재고보다 많이 입력하면 경고 + 판매 완료 버튼 비활성
    U->>P: 판매 완료 → 확인 팝업
    P->>K: POST /api/v1/sales (storeId는 ADMIN만, items)
    K->>S: create(loginUser, request) — @Valid 통과 후

    rect rgb(235, 242, 252)
        Note over S,D: @Transactional — 이 안에서 예외가 나면 전부 롤백
        S->>S: requireStoreId — MANAGER · USER는 소속 매장
        S->>S: 같은 상품 중복이면 400
        S->>R: requireActive(매장) · requireAvailable(상품들)
        R->>D: 매장 · 상품 조회 (사용 중지면 오류)
        S->>S: 단가 = 서버의 현재 판매가 (요청 값 사용 안 함), 합계 계산
        S->>N: saleNumber(매장코드, 판매 일시)
        N->>D: nextval('sale_number_seq')
        N-->>S: S-매장코드-yyyyMMdd-000001
        S->>D: INSERT sales RETURNING id
        S->>D: INSERT sale_items
        S->>T: change(sale, 상품별 − 수량)
        T->>D: 재고 행 잠금 → 차감 → 이력 (7장)
        alt 한 상품이라도 재고 부족
            T-->>S: BusinessException INSUFFICIENT_STOCK
            S-->>K: 예외 전파 → 롤백 (sales · sale_items 저장도 취소)
            K-->>P: 409 → useApiError가 메시지 표시
        else 정상
            T-->>S: 변동 결과
            S-->>K: 커밋 후 id · saleNumber
            K-->>P: 200
            P->>P: invalidateQueries() — 재고 · Dashboard 등 전부 새로 읽기
            P->>P: /sales/{id} 상세로 이동
        end
    end
```

- 화면의 재고 경고는 팝업을 연 시점의 수량 기준이다. 최종 판단은 7장의 잠금 후 검사가 한다. (그 사이 다른 판매가 있으면 409)
- 롤백되어도 `nextval`로 받은 번호는 되돌리지 않는다. 번호 중간이 비는 것은 허용하고, 중복이 없는 것을 우선한다.

## 9. 판매 취소 ★

```mermaid
flowchart TD
    req(["POST /api/v1/sales/{id}/cancel<br/>ADMIN · MANAGER (@PreAuthorize)"]) --> find["SaleMapper.findById"]
    find --> nf{"있음?"}
    nf -- 없음 --> e404(["404 SALE_NOT_FOUND"])
    nf -- 있음 --> scope{"checkStoreAccess<br/>내 매장의 판매?"}
    scope -- 아니오 --> e403(["403 FORBIDDEN"])
    scope -- 예 --> cond["조건부 UPDATE<br/>SET status = 'CANCELLED'<br/>WHERE id = ? AND status = 'COMPLETED'"]
    cond --> cnt{"바뀐 행 수"}
    cnt -- 0 --> e409(["409 SALE_ALREADY_CANCELLED"])
    cnt -- 1 --> items["판매 상세 조회"]
    items --> stock["StockService.change(saleCancel, 상품별 + 수량)<br/>재고 복원 · 이력 SALE_CANCEL"]
    stock --> ok(["200 CANCELLED · 커밋"])
```

- **같은 판매를 동시에 두 번 취소하면**: 먼저 온 UPDATE가 행을 잠그고, 나중 UPDATE는 기다렸다가 바뀐 상태(`CANCELLED`)를 보고 0행 → 409. 재고는 한 번만 복원된다.
- 상태를 먼저 읽고 나중에 바꾸는 방식(조회 → 확인 → 변경)을 쓰지 않는 이유가 이것이다. 조회와 변경 사이에 다른 취소가 끼어들 수 있다.

## 10. 발주 상태 전이와 입고 ★

```mermaid
stateDiagram-v2
    [*] --> DRAFT: 등록
    DRAFT --> DRAFT: 수정 (상품 전체 교체)
    DRAFT --> APPROVED: 승인
    APPROVED --> COMPLETED: 입고 ★ 재고 증가
    DRAFT --> CANCELLED: 취소
    APPROVED --> CANCELLED: 취소
    COMPLETED --> [*]
    CANCELLED --> [*]
```

수정 · 승인 · 입고 · 취소는 모두 `PurchaseOrderService.lockInStatus()`로 시작한다. 허용 상태만 다르다.

```mermaid
flowchart TD
    req(["POST /api/v1/purchase-orders/{id}/receive<br/>ADMIN · MANAGER"]) --> lock["PurchaseOrderMapper.lockById<br/>SELECT … FOR UPDATE (발주 행 잠금)"]
    lock --> nf{"있음?"}
    nf -- 없음 --> e404(["404 PURCHASE_ORDER_NOT_FOUND"])
    nf -- 있음 --> scope{"checkStoreAccess"}
    scope -- 다른 매장 --> e403(["403 FORBIDDEN"])
    scope -- 통과 --> st{"허용 상태인가?<br/>입고는 APPROVED만"}
    st -- 아니오 --> e409(["409 INVALID_PURCHASE_ORDER_STATUS"])
    st -- 예 --> comp["UPDATE status = COMPLETED<br/>completed_at · completed_by"]
    comp --> items["발주 상품 조회"]
    items --> stock["StockService.change(purchase, 상품별 + 수량)<br/>재고 증가 · 이력 PURCHASE"]
    stock --> ok(["200 COMPLETED · 커밋"])
```

| 처리 | 허용 상태 | 결과 |
|---|---|---|
| 수정 `PUT /{id}` | `DRAFT` | 발주 상품 전체 삭제 후 다시 저장, 합계 재계산 |
| 승인 `/approve` | `DRAFT` | `APPROVED` |
| 입고 `/receive` | `APPROVED` | `COMPLETED` + 재고 증가 |
| 취소 `/cancel` | `DRAFT`, `APPROVED` | `CANCELLED` |

- **입고와 취소가 동시에 오면**: 먼저 잠근 쪽이 처리하고, 나중 요청은 잠금이 풀린 뒤 바뀐 상태를 보고 409가 된다.
- 입고는 매장 · 상품이 그 사이 사용 중지되어도 처리한다. 이미 주문해서 도착한 물건이기 때문이다. 등록 · 수정 때만 사용 여부를 검사한다.

## 11. 매장 · 상품 등록과 재고 행 생성

```mermaid
flowchart TD
    a(["StoreService.create · ProductService.create<br/>@Transactional"]) --> lock["StockRowInitializer.lockRegistration<br/>pg_advisory_xact_lock(1001)"]
    lock --> ins["stores 또는 products INSERT"]
    ins --> rows["재고 행 생성 (수량 0)<br/>새 매장 × 모든 상품 / 새 상품 × 모든 매장"]
    rows --> commit(["커밋 → 잠금 자동 해제"])
```

- 매장 등록과 상품 등록이 잠금 없이 동시에 실행되면 서로의 커밋 전 데이터를 보지 못해 그 조합의 재고 행이 빠진다. 등록 작업끼리만 이 잠금으로 줄을 세운다.
- 그 결과 `stocks`에는 항상 매장 × 상품의 모든 조합이 있다. 그래서 7장은 행을 만들 필요 없이 **잠그기만** 하면 된다.

## 12. 트랜잭션 · 잠금 요약

| 상황 | 잠그는 대상 | 방법 | 동시에 요청하면 |
|---|---|---|---|
| 재고 변경 (판매 · 취소 · 입고 · 조정) | `stocks` 행 | `FOR UPDATE`, 상품 ID 오름차순 | 나중 요청은 대기 후 최신 수량으로 계산 → 부족하면 409 |
| 판매 취소 | `sales` 행 | 조건부 UPDATE (`WHERE status = 'COMPLETED'`) | 한 번만 성공, 재고 한 번만 복원 |
| 발주 수정 · 승인 · 입고 · 취소 | `purchase_orders` 행 | `FOR UPDATE` 후 상태 확인 | 하나만 성공 |
| 매장 · 상품 등록 | advisory lock `1001` | `pg_advisory_xact_lock` | 순서대로 실행, 재고 행 누락 없음 |
| ADMIN 강등 · 비활성화 | 활성 ADMIN 행 전체 | `FOR UPDATE` (`UserMapper.lockActiveAdminIds`) | 동시에 서로를 강등해도 활성 ADMIN 1명 이상 유지 |

- **잠금 순서는 항상 문서(판매 · 발주) → 재고(상품 ID 오름차순)** — 모든 업무가 같은 순서를 지키므로 교착 상태가 생기지 않는다.
- 위 동작은 `RealTransactionTestSupport`를 상속한 동시성 테스트가 실제 트랜잭션으로 검증한다. 잠금이나 조건을 지우면 해당 테스트가 실패한다.

## 13. 빌드 · 배포

```mermaid
flowchart LR
    push["git push · PR"] --> ci["GitHub Actions<br/>.github/workflows/ci.yml"]
    ci --> t1["Backend<br/>./gradlew test (Testcontainers)"]
    ci --> t2["Frontend<br/>Prettier · oxlint · 타입 체크 · 빌드"]
    t1 --> img["Docker 이미지 빌드<br/>backend · frontend, amd64 · arm64"]
    t2 --> img
    img -- "main 브랜치만" --> ghcr[("GHCR<br/>latest · sha-커밋")]
    ghcr -. "STEP 13 (예정)" .-> server["배포 서버<br/>docker compose pull · up"]
```

- 이미지 구성 · Nginx 설정은 [README 12장](../../README.md#12-docker), CI 설정은 [README 13장](../../README.md#13-cicd)을 본다.
