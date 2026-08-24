# user-service multi-stage Dockerfile
#
# Stage 1: Build the bootJar with Gradle on JDK 21.
# Stage 2: Run the resulting JAR on a slim JRE 21 image.
#
# Build:    docker build -f user-service/Dockerfile -t marketplace/user-service:latest .
# Run:      docker run --rm -p 8081:8081 marketplace/user-service:latest

# ---------- Build stage ----------
FROM gradle:8.10-jdk21-alpine AS build

WORKDIR /app

# Copy build configuration first to leverage Docker layer caching
COPY settings.gradle build.gradle gradle/ ./
COPY common/ common/
COPY user-service/ user-service/

RUN gradle :user-service:bootJar --no-daemon -x test

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=build /app/user-service/build/libs/user-service.jar app.jar

USER spring
EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]