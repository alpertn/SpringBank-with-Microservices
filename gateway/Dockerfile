# ── Stage 1: Maven dependency cache ──────────────────────────
# pom.xml AYRI kopyalanir, dependency'ler oncelikle indirilir.
# Boylece sadece pom.xml degistiginde dependency'ler yeniden indirilir.
# Kaynak kod degistiginde bu layer Docker CACHE'den gelir → build suresi 5-10x azalir.
# Ornek: Normalde her build 5-10 dk suren dependency indirme, cache sayesinde 0 sn olur.
FROM maven:3.9.6-eclipse-temurin-21-alpine AS deps
WORKDIR /app
COPY pom.xml .
# -B (batch mode): Maven'i interaktif olmayan modda calistirir (CI/CD uyumlu)
# dependency:go-offline + resolve: Tum bagimliliklari (BOM transitif dahil) indirir
# || true: go-offline bazi POM-only artifact'lari indiremiyor olsa da devam et
RUN mvn dependency:go-offline dependency:resolve dependency:resolve-plugins -B || true

# ── Stage 2: Build ───────────────────────────────────────────
# deps stage'inden devam eder — dependency'ler zaten indirilmis durumda
FROM deps AS build
COPY src ./src
# -DskipTests: Test calistirmadan paketler (CI pipeline'da testler ayri asamada calistirilir)
# Not: -o (offline) kaldirildi — BOM/transitif dep'ler go-offline ile tam indirilemiyor,
#      eksik artifact'lar icin Maven Maven Central'a baglanabilmeli.
RUN mvn clean package -DskipTests -B

# ── Stage 3: Runtime ─────────────────────────────────────────
# JRE-only image: JDK yerine sadece JRE kullanilir → image boyutu ~%50 kucuk
# alpine: Minimal Linux dagitimi → image boyutu daha da kucuk (~200MB vs ~400MB)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# kubectl kurulumu — LogStreamController tarafindan Kubernetes pod loglarini
# SSE (Server-Sent Events) olarak admin paneline stream etmek icin gerekli.
# Sadece gateway servisinde bulunur, diger servislerde kubectl gereksizdir.
RUN apk add --no-cache curl ca-certificates && \
    KUBECTL_VERSION=$(curl -L -s https://dl.k8s.io/release/stable.txt) && \
    curl -LO "https://dl.k8s.io/release/${KUBECTL_VERSION}/bin/linux/amd64/kubectl" && \
    chmod +x kubectl && \
    mv kubectl /usr/local/bin/kubectl

COPY --from=build /app/target/*.jar app.jar

# Docker HEALTHCHECK: Container icerisinden uygulamanin canli olup olmadigini kontrol eder.
# --interval=30s  : Her 30 saniyede bir kontrol yapar
# --timeout=5s    : 5 saniye icinde yanit gelmezse basarisiz sayar
# --start-period=40s : Uygulama baslayana kadar 40 saniye bekler (Spring Boot startup suresi)
# --retries=3     : 3 basarisiz denemeden sonra container "unhealthy" olarak isaretlenir
# /actuator/health/liveness: Spring Boot Actuator liveness probe endpoint'i
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8095/actuator/health/liveness || exit 1

# JVM Container-Aware Flags:
# -XX:+UseContainerSupport  : JVM'in container RAM limitini tanimasi (cgroup'tan okur).
#                              Bu olmadan JVM host makinenin RAM'ini gorur ve asiri bellek kullanir.
# -XX:MaxRAMPercentage=75.0 : Container RAM limitinin %75'ini JVM heap icin kullanir.
#                              Kalan %25: Metaspace, thread stack, native memory, OS islemleri.
#                              Ornek: 512MB container → 384MB heap, 128MB sistem.
# -Djava.security.egd       : JVM baslangicinda /dev/random yerine /dev/urandom kullanir.
#                              Entropi bekleme suresini ortadan kaldirir → startup 2-5 sn hizlanir.
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
