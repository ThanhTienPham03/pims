FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Build executable JAR
COPY src ./src
RUN mvn clean package -DskipTests -B

# ----------------- STAGE 2: Runtime Image -----------------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Set container timezone to Asia/Ho_Chi_Minh
RUN apk add --no-cache tzdata curl && \
    cp /usr/share/zoneinfo/Asia/Ho_Chi_Minh /etc/localtime && \
    echo "Asia/Ho_Chi_Minh" > /etc/timezone

# Create non-root user
RUN addgroup -S pims && adduser -S pims -G pims
USER pims

# Copy fat jar from builder
COPY --from=builder /build/target/*.jar app.jar

# Expose Spring Boot port
EXPOSE 8085

# Java execution options
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -Dfile.encoding=UTF-8"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
