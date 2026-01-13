# =============================================================================
# Dockerfile for Catalog Service
# =============================================================================

# -----------------------------------------------------------------------------
# Stage 1: Build with Maven
# -----------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy pom files first for dependency caching
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build without tests
RUN mvn clean package -DskipTests

# Extract layers for better caching
RUN java -Djarmode=layertools -jar target/*.jar extract --destination target/extracted

# -----------------------------------------------------------------------------
# Stage 2: Runtime
# -----------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine

# Install curl for health checks and set timezone
RUN apk add --no-cache curl tzdata \
    && cp /usr/share/zoneinfo/Asia/Ho_Chi_Minh /etc/localtime \
    && echo "Asia/Ho_Chi_Minh" > /etc/timezone \
    && apk del tzdata

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

# Create directories for logs and temp files
RUN mkdir -p /app/logs /app/tmp && chown -R spring:spring /app

# Copy layers from builder (order matters for caching)
COPY --from=builder --chown=spring:spring /build/target/extracted/dependencies/ ./
COPY --from=builder --chown=spring:spring /build/target/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=spring:spring /build/target/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=spring:spring /build/target/extracted/application/ ./

# Switch to non-root user
USER spring

# Expose port
EXPOSE 9002

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:9002/actuator/health || exit 1

# JVM settings for container
ENV JAVA_OPTS="-XX:+UseG1GC \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:+UseContainerSupport \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/app/logs/heapdump.hprof \
    -Djava.io.tmpdir=/app/tmp \
    -Djava.security.egd=file:/dev/./urandom"

# Run using layered jar launcher
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
