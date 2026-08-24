# notification-service multi-stage Dockerfile
#
# Stage 1: Build the bootJar with Gradle on JDK 21.
# Stage 2: Run the resulting JAR on a slim JRE 21 image.
#
# Build:    docker build -f notification-service/Dockerfile -t marketplace/notification-service:latest .
# Run:      docker run --rm -p 8084:8084 marketplace/notification-service:latest

# ---------- Build stage ----------
FROM gradle:8.10-jdk21-alpine AS build

WORKDIR /app

# Copy build configuration first to leverage Docker layer caching
COPY settings.gradle build.gradle gradle/ ./
COPY common/ common/
COPY notification-service/ notification-service/

RUN gradle :notification-service:bootJar --no-daemon -x test

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=build /app/notification-service/build/libs/notification-service.jar app.jar

USER spring
EXPOSE 8084

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
