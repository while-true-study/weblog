# FE build
FROM node:20-alpine AS fe-builder
WORKDIR /fe

COPY FE/package*.json ./
RUN npm ci

# 소스 복사 빌드
COPY FE/ .
RUN npm run build


# 2) BE build
FROM gradle:8.7-jdk17 AS builder
WORKDIR /app

COPY build.gradle* settings.gradle* gradlew gradlew.bat /app/
COPY gradle /app/gradle
COPY src /app/src

# FE 빌드 복사
ARG FE_BUILD_DIR=dist
COPY --from=fe-builder /fe/${FE_BUILD_DIR} /app/src/main/resources/static

# 테스트는 CI에서 이미 하니 이미지 빌드에서는 생략
RUN chmod +x ./gradlew && ./gradlew clean bootJar -x test

# Run
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENV TZ=Asia/Seoul
ENTRYPOINT ["java","-jar","/app/app.jar"]
