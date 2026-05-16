# -- Stage 1: Maven dependency cache ------------------------------------------------------------
# Alpine yerine Debian-slim kullanilir: protoc binary glibc gerektirir, Alpine musl kullanir
# protoc-gen-grpc-java .exe uzantili binary indirir, Debian'da calisir Alpine'de calistirilamaz
FROM maven:3.9.6-eclipse-temurin-21 AS deps
WORKDIR /app
COPY pom.xml .
# dependency'leri ve protoc binary'i indir (|| true: bazi artifact'lar eksik olabilir, devam et)
RUN mvn dependency:go-offline -B || true
# Protoc binary'lere calistirma izni ver (.exe = ELF binary, Linux'ta chmod +x gerekiyor)
RUN find /root/.m2 -name "*.exe" -exec chmod +x {} \;

# -- Stage 2: Build -----------------------------------------------------------------------------
FROM deps AS build
COPY src ./src
# Protoc binary'lere tekrar izin ver, ardindan build et
RUN find /root/.m2 -name "*.exe" -exec chmod +x {} \; && mvn clean package -DskipTests

# -- Stage 3: Runtime ---------------------------------------------------------------------------
# JRE-only image kullanilir; JDK araclari olmadigi icin image daha kucuk olur.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Healthcheck command-side'in ayakta olup olmadigini kontrol eder.
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8092/api/money-service-command/v1/accounts/health || exit 1

# JVM Container-Aware Flags:
# -XX:+UseContainerSupport  : JVM container limitlerini dogru algilar
# -XX:MaxRAMPercentage=75.0 : Heap icin container RAM'inin kontrollu kullanimi
# -Djava.security.egd       : Startup sirasinda daha hizli entropy kaynagi kullanir
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
