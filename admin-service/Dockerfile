# admin-service multi-stage Dockerfile
#
# Stage 1: Build the bootJar with Gradle on JDK 21.
# Stage 2: Run the resulting JAR on a slim JRE 21 image.
#
# Build:    docker build -f admin-service/Dockerfile -t marketplace/admin-service:latest .
# Run:      docker run --rm -p 8085:8085 marketplace/admin-service:latest

# ---------- Build stage ----------
FROM gradle:8.10-jdk21-alpine AS build

WORKDIR /app

# Copy build configuration explicitly (Docker on Windows quirk: COPY gradle/
# does not pick up the version-catalog TOML — must enumerate files).
COPY settings.gradle build.gradle ./
COPY gradle/libs.versions.toml gradle/
COPY gradle/wrapper/ gradle/wrapper/
COPY common/ common/
COPY admin-service/ admin-service/

RUN gradle :admin-service:bootJar --no-daemon -x test

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=build /app/admin-service/build/libs/admin-service.jar app.jar

USER spring
EXPOSE 8085

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
