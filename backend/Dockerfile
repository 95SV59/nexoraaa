# =============================================================================
# Stage 1: Build JAR using Maven
# =============================================================================
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build executable JAR
COPY src ./src
RUN mvn clean package -DskipTests -B

# =============================================================================
# Stage 2: Minimal JRE Runtime
# =============================================================================
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Create non-root system user for security
RUN groupadd -r nexora && useradd -r -g nexora nexora

COPY --from=builder /app/target/*.jar app.jar
RUN chown -R nexora:nexora /app

USER nexora
EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
