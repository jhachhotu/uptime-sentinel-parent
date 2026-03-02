# ── Build Stage ──
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy parent POM and all module POMs for dependency caching
COPY pom.xml .
COPY common/pom.xml common/
COPY monitoring-service/pom.xml monitoring-service/
COPY registry-service/pom.xml registry-service/
COPY config-server/pom.xml config-server/
COPY api-gateway/pom.xml api-gateway/

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -pl monitoring-service -am -B 2>/dev/null || true

# Copy source code
COPY common/src common/src
COPY monitoring-service/src monitoring-service/src

# Build only monitoring-service (and its dependency: common)
RUN mvn package -pl monitoring-service -am -DskipTests -B

# ── Runtime Stage ──
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=build /app/monitoring-service/target/*.jar app.jar

EXPOSE ${PORT:-8081}

# Use explicit memory limits to prevent OOM on Render free tier (512MB limit)
ENTRYPOINT ["java", "-Xmx300m", "-Xss512k", "-XX:CICompilerCount=2", "-Djava.net.preferIPv4Stack=true", "-jar", "app.jar"]
