# eureka-server multi-stage Dockerfile
#
# Stage 1: Build the bootJar with Gradle on JDK 21.
# Stage 2: Run the resulting JAR on a slim JRE 21 image.
#
# Build:    docker build -f eureka-server/Dockerfile -t marketplace/eureka-server:latest .
# Run:      docker run --rm -p 8761:8761 marketplace/eureka-server:latest

# ---------- Build stage ----------
FROM gradle:8.10-jdk21-alpine AS build

WORKDIR /app

# Copy build configuration first to leverage Docker layer caching
COPY settings.gradle build.gradle gradle/ ./
COPY eureka-server/ eureka-server/

RUN gradle :eureka-server:bootJar --no-daemon -x test

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=build /app/eureka-server/build/libs/eureka-server.jar app.jar

USER spring
EXPOSE 8761

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
