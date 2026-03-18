FROM gradle:8.10.1-jdk17 AS build
WORKDIR /deliveryCo

COPY --chown=gradle:gradle . .
RUN gradle --no-daemon clean bootJar -x test
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN groupadd -r spring && useradd -r -g spring spring
COPY --from=build /deliveryCo/build/libs/*.jar /app/app.jar

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseContainerSupport"

EXPOSE 8084
USER spring

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
