# api-gateway multi-stage Dockerfile
#
# Stage 1: Build the bootJar with Gradle on JDK 21.
# Stage 2: Run the resulting JAR on a slim JRE 21 image.
#
# Build:    docker build -f api-gateway/Dockerfile -t marketplace/api-gateway:latest .
# Run:      docker run --rm -p 8080:8080 marketplace/api-gateway:latest

# ---------- Build stage ----------
FROM gradle:8.10-jdk21-alpine AS build

WORKDIR /app

# Copy build configuration explicitly (Docker on Windows quirk: COPY gradle/
# does not pick up the version-catalog TOML — must enumerate files).
COPY settings.gradle build.gradle ./
COPY gradle/libs.versions.toml gradle/
COPY gradle/wrapper/ gradle/wrapper/
COPY api-gateway/ api-gateway/

RUN gradle :api-gateway:bootJar --no-daemon -x test

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=build /app/api-gateway/build/libs/api-gateway.jar app.jar

USER spring
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
