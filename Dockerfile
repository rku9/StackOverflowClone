# syntax=docker/dockerfile:1

# -----------------
# Build stage
# -----------------
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Cache dependencies first
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests dependency:go-offline

# Copy source and build
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests package

# -----------------
# Runtime stage
# -----------------
FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app

# Create non-root user
RUN useradd -m -u 1001 -s /bin/bash appuser

# Copy shaded/boot jar from build stage
COPY --from=build /app/target/*.jar /app/app.jar

# Prepare writable directories for logs and app data
RUN mkdir -p /app/logs && chown -R appuser:appuser /app

# Expose default port (Render sets $PORT at runtime; we still expose 8080 for local)
EXPOSE 8080

# Allow passing extra JVM flags at runtime
ENV JAVA_OPTS=""
ENV PORT=8080

USER appuser

# Bind Spring Boot to the PORT provided by Render
CMD ["sh", "-c", "java $JAVA_OPTS -Dserver.port=$PORT -Dlogging.file.name=/dev/stdout -jar /app/app.jar"]
