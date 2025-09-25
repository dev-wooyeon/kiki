# 빌드 스테이지
FROM gradle:8.9-jdk17 AS build
WORKDIR /workspace
COPY . .
RUN gradle build -x test --no-daemon

# 런타임 스테이지
FROM amazoncorretto:17 AS runtime
WORKDIR /app

# 빌드 결과 복사
COPY --from=build /workspace/build/libs/*.jar /app/app.jar

# 환경 변수 & 포트
ENV SERVER_PORT=8080
EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]