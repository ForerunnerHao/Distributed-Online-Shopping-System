# Stage 1: Build
FROM gradle:8.10.1-jdk17 AS builder

WORKDIR /build

# Copy Gradle files
COPY gradle ./gradle
COPY gradlew ./
COPY gradlew.bat ./
COPY build.gradle ./
COPY settings.gradle ./

# Copy source
COPY src ./src

# Build application (skip tests in Docker build)
RUN chmod +x ./gradlew && ./gradlew clean bootJar -x test

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN groupadd -r spring && useradd -r -g spring spring

# Copy JAR from builder
COPY --from=builder /build/build/libs/*.jar app.jar

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseContainerSupport"

EXPOSE 8086

USER spring

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

