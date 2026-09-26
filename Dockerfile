# ---- Backend: multi-stage build ----
# Stage 1: build the Spring Boot fat jar with Maven + JDK 21.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache dependencies first (pom changes rarely relative to source).
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

# Build the application (skip tests in the image build; run them in CI instead).
COPY src ./src
RUN mvn -q -B clean package -DskipTests

# Stage 2: slim runtime with just a JRE.
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

# Non-root user for safety.
RUN groupadd -r chargemap && useradd -r -g chargemap chargemap

COPY --from=build /app/target/chargemap-api-*.jar app.jar
RUN chown -R chargemap:chargemap /app
USER chargemap

EXPOSE 8080

# Container-friendly JVM flags; honor MaxRAMPercentage of the container memory limit.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseContainerSupport"

# Health: Spring Boot actuator health endpoint.
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
