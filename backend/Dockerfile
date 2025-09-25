# Multi-stage build for Spring Boot (JDK 17)

FROM gradle:8.7-jdk17-alpine AS build
WORKDIR /workspace
COPY . .
RUN gradle :build -x test --no-daemon

FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

# Copy built jar
COPY --from=build /workspace/build/libs/*.jar /app/app.jar

# Environment (override in runtime via env vars)
ENV SERVER_PORT=8080
EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]

