# ── Stage 1: Maven dependency cache ──────────────────────────
# pom.xml AYRI kopyalanir, dependency'ler oncelikle indirilir.
# Boylece sadece pom.xml degistiginde dependency'ler yeniden indirilir.
# Kaynak kod degistiginde bu layer Docker CACHE'den gelir → build suresi 5-10x azalir.
FROM maven:3.9.6-eclipse-temurin-21 AS deps
WORKDIR /app
COPY pom.xml .
# -B (batch mode): Maven'i interaktif olmayan modda calistirir (CI/CD uyumlu)
# dependency:go-offline: Tum bagimliliklari indirir ama derleme yapmaz
# || true: Bazi gRPC artifact'lari offline cache'e girmez, hata olsa bile devam et
RUN mvn dependency:go-offline -B || true
# protoc-gen-grpc-java Linux binary'si .exe uzantili gelir; Docker build icinde calisabilmesi icin izin ver.
RUN find /root/.m2 -name "*.exe" -exec chmod +x {} \;

# ── Stage 2: Build ───────────────────────────────────────────
# deps stage'inden devam eder — dependency'ler zaten indirilmis durumda
FROM deps AS build
COPY src ./src
# Online mode: Alpine'da protoc binary calismiyordu; Debian-slim ile sorun cozuldu
RUN find /root/.m2 -name "*.exe" -exec chmod +x {} \; && mvn clean package -DskipTests

# ── Stage 3: Runtime ─────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Docker HEALTHCHECK: Her 30 saniyede uygulamanin canli olup olmadigini kontrol eder.
# --start-period=40s : Spring Boot baslayana kadar bekler (startup suresi)
# --retries=3        : 3 basarisiz denemeden sonra container "unhealthy" olarak isaretlenir
# /actuator/health/liveness : Kubernetes liveness probe endpoint'i (application.yaml'da aktif)
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/api/user-service/actuator/health/liveness || exit 1

# JVM Container-Aware Flags:
# -XX:+UseContainerSupport  : JVM'in container RAM limitini tanimasi (OOMKill onlemi)
# -XX:MaxRAMPercentage=75.0 : Container RAM'inin %75'ini heap, %25'ini OS/native icin ayirir
# -Djava.security.egd       : /dev/urandom kullanarak JVM startup suresini 2-5 sn kisaltir
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
