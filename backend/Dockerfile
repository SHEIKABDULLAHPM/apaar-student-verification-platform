# Multi-stage Dockerfile for the APAAR Java Backend
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace

# Copy POM first so dependency resolution can be reused across source changes
COPY backend/pom.xml backend/pom.xml
WORKDIR /workspace/backend

# Copy only build inputs required by the Maven resources plugin. Synthetic demo
# spreadsheets are not needed to build the jar and would otherwise invalidate the
# dependency cache whenever they are regenerated.
COPY database/migrations /workspace/database/migrations
COPY database/seed /workspace/database/seed
COPY backend/src src

# Build final jar. The BuildKit cache mount persists the Maven repository across
# builds without keeping a stale dependency layer tied to the source tree.
RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests -B -ntp

# Runtime image
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Run as non-root user
RUN addgroup -S apaar && adduser -S apaar -G apaar \
    && mkdir -p /app/data/uploads /tmp/apaar-uploads \
    && chown -R apaar:apaar /app /tmp/apaar-uploads
USER apaar

COPY --from=builder /workspace/backend/target/apaar-platform-backend.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -Duser.timezone=UTC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

