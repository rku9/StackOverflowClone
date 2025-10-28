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
CMD ["sh", "-c", "set -e; \
 DB_URL_OPT=\"\"; USER_OPT=\"\"; PASS_OPT=\"\"; \
 if [ -n \"$DB_URL\" ]; then \
   case \"$DB_URL\" in \
     postgres://*|postgresql://*) \
       URL_NO_SCHEME=${DB_URL#*://}; \
       if echo \"$URL_NO_SCHEME\" | grep -q \"@\"; then \
         CREDS=${URL_NO_SCHEME%@*}; HOSTPATH=${URL_NO_SCHEME#*@}; \
         JDBC_URL=\"jdbc:postgresql://$HOSTPATH\"; \
         [ -z \"$DB_USER\" ] && USER_OPT=\"-Dspring.datasource.username=${CREDS%%:*}\"; \
         [ -z \"$DB_PASSWORD\" ] && PASS_OPT=\"-Dspring.datasource.password=${CREDS#*:}\"; \
       else \
         JDBC_URL=\"jdbc:postgresql://$URL_NO_SCHEME\"; \
       fi; \
       DB_URL_OPT=\"-Dspring.datasource.url=$JDBC_URL\"; \
       ;; \
   esac; \
 fi; \
 exec java $JAVA_OPTS -Dserver.port=$PORT -Dlogging.file.name=/dev/stdout $DB_URL_OPT $USER_OPT $PASS_OPT -jar /app/app.jar"]
