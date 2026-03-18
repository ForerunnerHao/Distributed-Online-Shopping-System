# Stage 1: Build
FROM gradle:8.10.1-jdk17 AS builder

WORKDIR /build

# Copy Gradle files (including root build.gradle which defines Spring Boot plugin)
COPY build.gradle build.gradle
COPY settings.gradle settings.gradle
COPY gradle gradle
COPY gradlew gradlew
COPY gradlew.bat gradlew.bat

# Copy all project modules
COPY common common
COPY proto proto
COPY store-app/build.gradle store-app/build.gradle
COPY store-app/src store-app/src

# Build application (skip tests in Docker build)
RUN chmod +x gradlew && ./gradlew :store-app:clean :store-app:bootJar -x test

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN groupadd -r spring && useradd -r -g spring spring

# Copy JAR from builder
COPY --from=builder /build/store-app/build/libs/*.jar app.jar

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseContainerSupport"

EXPOSE 8080

USER spring

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

