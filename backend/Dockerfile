# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/bookshop-api-1.0.0.jar app.jar
# Tuned for a small free-tier container (512 MB RAM, fractional CPU) where
# cold-start latency matters far more than peak throughput:
#   MaxRAMPercentage   - keep the heap inside the memory limit
#   TieredStopAtLevel=1 - C1 only; much faster warm-up, lower peak performance
#   UseSerialGC        - least GC overhead when there is barely one core
#   spring.jmx.enabled - nothing here reads JMX; skip registering the beans
ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=70", \
  "-XX:TieredStopAtLevel=1", \
  "-XX:+UseSerialGC", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-Dspring.jmx.enabled=false", \
  "-jar", "app.jar"]
