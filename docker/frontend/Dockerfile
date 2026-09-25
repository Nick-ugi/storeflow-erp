# syntax=docker/dockerfile:1
# StoreFlow Frontend 이미지: React 빌드 결과를 Nginx가 제공하고 /api 요청은 백엔드로 넘긴다.
# 빌드 컨텍스트는 저장소 루트다.
#   docker compose -f docker/docker-compose.yml build frontend

# ---- 1단계: 타입 체크 + 프로덕션 빌드 ----
FROM node:24-alpine AS build
WORKDIR /app

COPY frontend/package.json frontend/package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci --no-audit --no-fund

COPY frontend/ ./
RUN npm run build

# ---- 2단계: Nginx ----
FROM nginx:1.30-alpine

# 백엔드 주소(host:port). 템플릿의 ${BACKEND_ADDR}에 들어간다.
# NGINX_ENTRYPOINT_LOCAL_RESOLVERS: 컨테이너의 DNS 서버를 ${NGINX_LOCAL_RESOLVERS}로 넘겨 준다.
ENV BACKEND_ADDR=backend:8080 \
    NGINX_ENTRYPOINT_LOCAL_RESOLVERS=1

# /etc/nginx/templates/*.template → 기동 시 환경변수를 채워 /etc/nginx/conf.d/에 생성된다.
COPY docker/frontend/default.conf.template /etc/nginx/templates/default.conf.template
COPY docker/frontend/security-headers.conf /etc/nginx/snippets/security-headers.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
