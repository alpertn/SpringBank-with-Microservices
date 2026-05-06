# -- Stage 1: Maven dependency cache ------------------------------------------------------------
# pom.xml AYRI kopyalanir, dependency'ler once indirilir.
# Boylece sadece pom.xml degistiginde dependency'ler yeniden indirilir.
# Kaynak kod degistiginde bu layer Docker cache'den gelir ve build hizlanir.
FROM maven:3.9.6-eclipse-temurin-21-alpine AS deps
WORKDIR /app
COPY pom.xml .
# -B (batch mode): Maven'i CI/CD uyumlu interaktif olmayan modda calistirir.
# dependency:go-offline: Build oncesi tum dependency ve plugin'leri indirir.
RUN mvn dependency:go-offline -B

# -- Stage 2: Build -----------------------------------------------------------------------------
FROM deps AS build
COPY src ./src
# -o (offline): Stage 1'de inen dependency cache'ini kullanir.
RUN mvn clean package -DskipTests -o

# -- Stage 3: Runtime ---------------------------------------------------------------------------
# JRE-only image kullanilir; JDK araclari olmadigi icin image daha kucuk olur.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Healthcheck query-side'in ayakta olup olmadigini kontrol eder.
# Bu serviste garanti olan endpoint custom /health endpoint'idir.
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8093/api/money-service-query/v1/accounts/health || exit 1

# JVM Container-Aware Flags:
# -XX:+UseContainerSupport  : JVM container limitlerini dogru algilar
# -XX:MaxRAMPercentage=75.0 : Heap icin container RAM'inin kontrollu kullanimi
# -Djava.security.egd       : Startup sirasinda daha hizli entropy kaynagi kullanir
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
