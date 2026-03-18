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
COPY warehouse/build.gradle warehouse/build.gradle
COPY warehouse/src warehouse/src

# Build application (will be used for both warehouse1 and warehouse2)
RUN chmod +x gradlew && ./gradlew :warehouse:clean :warehouse:bootJar -x test

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN groupadd -r spring && useradd -r -g spring spring

# Copy JAR from builder
COPY --from=builder /build/warehouse/build/libs/*.jar app.jar

# Install unzip for extracting JAR if needed
RUN apt-get update && apt-get install -y unzip bash && rm -rf /var/lib/apt/lists/*

# Create startup script that supports both warehouse1 and warehouse2
RUN echo '#!/bin/bash\n\
set -e\n\
if [ "$WAREHOUSE_MAIN_CLASS" = "Tutorial7_8.warehouse02.Warehouse2Application" ]; then\n\
    # For warehouse2, extract JAR and run with custom main class\n\
    mkdir -p /tmp/warehouse-extracted\n\
    cd /tmp/warehouse-extracted\n\
    unzip -q -o /app/app.jar\n\
    # Build classpath from extracted JAR structure\n\
    CLASSPATH="/tmp/warehouse-extracted/BOOT-INF/classes"\n\
    for jar in /tmp/warehouse-extracted/BOOT-INF/lib/*.jar; do\n\
        CLASSPATH="$CLASSPATH:$jar"\n\
    done\n\
    exec java $JAVA_OPTS -cp "$CLASSPATH" Tutorial7_8.warehouse02.Warehouse2Application --spring.profiles.active=$WAREHOUSE_PROFILE "$@"\n\
else\n\
    # Default: use the main class specified in JAR manifest (warehouse1)\n\
    exec java $JAVA_OPTS -jar /app/app.jar --spring.profiles.active=$WAREHOUSE_PROFILE "$@"\n\
fi' > /app/start.sh && chmod +x /app/start.sh

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseContainerSupport"

# Default to warehouse1, can be overridden in docker-compose
ENV WAREHOUSE_PROFILE="warehouse1"
ENV WAREHOUSE_MAIN_CLASS=""

EXPOSE 9091 8081 9092 8082

USER spring

ENTRYPOINT ["/app/start.sh"]

