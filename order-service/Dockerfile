# order-service multi-stage Dockerfile
#
# Stage 1: Build the bootJar with Gradle on JDK 21.
# Stage 2: Run the resulting JAR on a slim JRE 21 image.
#
# Build:    docker build -f order-service/Dockerfile -t marketplace/order-service:latest .
# Run:      docker run --rm -p 8083:8083 marketplace/order-service:latest

# ---------- Build stage ----------
FROM gradle:8.10-jdk21-alpine AS build

WORKDIR /app

# Copy build configuration first to leverage Docker layer caching
COPY settings.gradle build.gradle gradle/ ./
COPY common/ common/
COPY order-service/ order-service/

RUN gradle :order-service:bootJar --no-daemon -x test

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=build /app/order-service/build/libs/order-service.jar app.jar

USER spring
EXPOSE 8083

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
