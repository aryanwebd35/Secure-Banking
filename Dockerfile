# ─── Stage 1: Build ───────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# Copy pom.xml first for dependency layer caching
COPY pom.xml ./

# Download all dependencies (cached unless pom.xml changes)
RUN mvn dependency:go-offline -B

# Copy source code and build the JAR (skip tests — tests need a running DB)
COPY src ./src
RUN mvn clean package -DskipTests -B

# ─── Stage 2: Run ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy only the built JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose the port Spring Boot listens on
EXPOSE 8080

# Start the application
ENTRYPOINT ["java", "-jar", "app.jar"]
