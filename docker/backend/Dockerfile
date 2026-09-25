# syntax=docker/dockerfile:1
# StoreFlow Backend (Spring Boot) 이미지. 빌드 컨텍스트는 저장소 루트다.
#   docker compose -f docker/docker-compose.yml build backend

# ---- 1단계: 실행 jar 빌드 ----
# 테스트는 여기서 돌리지 않는다. Testcontainers가 Docker를 필요로 하므로 CI에서 따로 실행한다.
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY backend/gradlew backend/settings.gradle backend/build.gradle ./
COPY backend/gradle gradle
COPY backend/src src

# Gradle 배포판 · 의존성은 캐시 마운트에 보관해 다시 빌드할 때 내려받지 않는다.
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon bootJar \
    && cp build/libs/storeflow-backend-*.jar app.jar \
    && java -Djarmode=tools -jar app.jar extract --layers --destination extracted

# ---- 2단계: 실행 이미지 (JRE만) ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S storeflow && adduser -S -G storeflow storeflow

# 자주 바뀌지 않는 층부터 복사한다. 코드만 바뀌면 마지막 application 층만 새로 받는다.
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

USER storeflow

# prod 프로필: 개발용 JWT 키(application-local.yml)를 쓰지 않는다. JWT_SECRET이 없으면 기동에 실패한다.
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080
# 힙 상한은 컨테이너 메모리 기준 75%. 추가 JVM 옵션은 JAVA_TOOL_OPTIONS로 넘긴다.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
