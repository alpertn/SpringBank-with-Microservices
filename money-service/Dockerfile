# ── Stage 1: Maven dependency cache ──────────────────────────
# pom.xml AYRI kopyalanir, dependency'ler oncelikle indirilir.
# Boylece sadece pom.xml degistiginde dependency'ler yeniden indirilir.
# Kaynak kod degistiginde bu layer Docker CACHE'den gelir → build suresi 5-10x azalir.
FROM maven:3.9.6-eclipse-temurin-21-alpine AS deps
WORKDIR /app
COPY pom.xml .
# -B (batch mode): Maven'i interaktif olmayan modda calistirir (CI/CD uyumlu)
# dependency:go-offline: Tum bagimliliklari indirir ama derleme yapmaz
RUN mvn dependency:go-offline -B

# ── Stage 2: Build ───────────────────────────────────────────
FROM deps AS build
COPY src ./src
# -o (offline): Internete cikmaz, Stage 1'deki cache'i kullanir
RUN mvn clean package -DskipTests -o

# ── Stage 3: Runtime ─────────────────────────────────────────
# JRE-only + alpine: Sadece calisma zamani, JDK araclari haric → image ~%50 kucuk
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Docker HEALTHCHECK: Her 30 saniyede uygulamanin canli olup olmadigini kontrol eder.
# --start-period=40s : Spring Boot baslayana kadar bekler
# /actuator/health/liveness : Kubernetes liveness probe endpoint'i
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8082/api/money-service/actuator/health/liveness || exit 1

# JVM Container-Aware Flags:
# -XX:+UseContainerSupport  : JVM'in container RAM limitini tanimasi (OOMKill onlemi)
# -XX:MaxRAMPercentage=75.0 : Container RAM'inin %75'ini heap, %25'ini OS/native icin ayirir
# -Djava.security.egd       : /dev/urandom kullanarak JVM startup suresini 2-5 sn kisaltir
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
