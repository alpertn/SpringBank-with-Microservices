# -- Stage 1: Maven dependency cache ------------------------------------------------------------
FROM maven:3.9.6-eclipse-temurin-21 AS deps
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B || true
RUN find /root/.m2 -name "*.exe" -exec chmod +x {} \;

# -- Stage 2: Build -----------------------------------------------------------------------------
FROM deps AS build
COPY src ./src
RUN find /root/.m2 -name "*.exe" -exec chmod +x {} \; && mvn clean package -DskipTests

# -- Stage 3: Runtime ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8098/api/admin-service-query/v1/history/health || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
