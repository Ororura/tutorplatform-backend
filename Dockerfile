FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /workspace
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon
COPY src ./src
RUN ./gradlew clean build -x test --no-daemon

FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S app \
    && adduser -S app -G app \
    && mkdir -p /var/lib/tutor/files \
    && chown -R app:app /var/lib/tutor
WORKDIR /app
COPY --from=build --chown=app:app /workspace/build/libs/*.jar app.jar
USER app
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -q -T 4 -O /dev/null http://127.0.0.1:8080/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
