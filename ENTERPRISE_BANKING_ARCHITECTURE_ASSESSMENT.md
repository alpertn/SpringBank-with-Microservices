# SpringBank Kurumsal Bankacilik Mimari Degerlendirmesi

**Belge durumu:** Mevcut kod ve konfigurasyona dayali mimari inceleme, hedef mimari ve donusum plani
**Inceleme tarihi:** 24 Eylul 2026
**Kapsam:** 13 Spring Boot mikroservisi, HTTP/gRPC/Kafka akislari, veri depolari, Keycloak, Kubernetes, CI/CD, testler ve mimari dokumantasyon
**Iliskili belgeler:** `DATA_FLOW_ARCHITECTURE.md`, `CODEX_PROJECT_MEMORY.md`, `CUSTOMER_SERVICES_IMPLEMENTATION_PLAN.md`, `readme/current-data-flow-architecture.drawio`

## 1. Yonetici Ozeti

Tam hedef high-scale banka domain haritasi, 37 uctan uca referans akis, platform,
guvenlik, veri, operasyon ve production-readiness kurallari ayri olarak
`HIGH_SCALE_BANKING_REFERENCE_ARCHITECTURE.md` belgesinde tanimlanmistir. Bu
belge ise mevcut SpringBank kodunun o hedefe gore durumunu ve donusum onceligini
gosterir.

SpringBank, Java 21, Spring Boot, servis basina veri ayrimi, Kafka tabanli asenkron akislari, gRPC, Keycloak ve Kubernetes kullanan gelismis bir mikroservis prototipidir. Customer command/query servislerinde audit alanlari, optimistic locking, soft delete ve ayri projection modelleri gibi dogru temeller bulunmaktadir. Buna ragmen sistem bugunku haliyle kurumsal bir bankanin cekirdek para sistemi olarak siniflandirilamaz.

Temel fark teknoloji isimleri degil, finansal ve operasyonel garantilerdir. Kurumsal bir banka sistemi para hareketinde degismez cift tarafli defter, atomik posting, para birimi bazinda denge, idempotency, ters kayitla duzeltme, mutabakat ve kanitlanabilir audit ister. SpringBank ise halen `money` ve `blockedMoney` alanlarini dogrudan guncelleyen iki farkli money write modeli tasimaktadir.

En kritik sonuc sudur: once yeni mikroservis eklemek yerine para ve musteri verisinin tek sahipligini kesinlestirmek, database-Kafka dual-write sorununu outbox/inbox ile gidermek, mesaj sozlesmelerini merkezilestirmek ve servisler arasi guveni kimlik tabanli hale getirmek gerekir. Aksi halde servis sayisi artarken finansal dogruluk ve ariza anindaki toparlanma zayiflar.

### 1.1 Mevcut olgunluk ozeti

| Alan | Seviye | Degerlendirme |
|---|---:|---|
| Domain ayrimi | 2.5/5 | Identity, customer, money, transaction, fraud ve admin ayrilmis; ancak money sahipligi cakismali |
| Finansal ledger | 1/5 | BigDecimal var; cift tarafli, degismez journal ve posting garantileri yok |
| Asenkron tutarlilik | 1.5/5 | Kafka ve bazi idempotency kayitlari var; outbox/inbox, DLT ve sema yonetimi yok |
| API ve DTO yonetisimi | 2/5 | DTO kullanan yerler var; persistence model sizintisi ve kopya integration DTO'lari bulunuyor |
| Guvenlik | 1.5/5 | Keycloak/JWT var; servis kimligi, mTLS, secret yonetimi ve least privilege eksik |
| Dayaniklilik | 1/5 | Tek replica altyapi, Kafka RF=1 ve tanimli RPO/RTO/SLO yok |
| Gozlemlenebilirlik | 1/5 | Actuator ve loglar var; distributed tracing, metrik standardi ve korelasyon yok |
| Test guvencesi | 1.5/5 | Unit testler var; entegrasyon, contract, concurrency, load, chaos ve DR testleri yok |
| CI/CD ve tedarik zinciri | 2/5 | Gitleaks, Maven, JaCoCo ve Sonar temeli var; SBOM, image scan, imza ve provenance eksik |
| Mimari gorunurluk | 2.5/5 | Draw.io ve veri akisi belgesi var; bu calismada EventCatalog ve jQAssistant temeli eklendi |

Bu puanlar uyumluluk sertifikasi degildir. Kod tabani uzerinden teknik risk onceliklendirmesidir. Gercek kapasite ve dayaniklilik seviyesi ancak yuk, kaos, geri yukleme ve bolge kaybi testleriyle olculebilir.

## 2. Inceleme Yontemi

Inceleme yalnizca klasor isimlerine bakilarak yapilmadi. Controller, service, helper, repository, entity, DTO, proto, Kafka publisher/listener, application YAML, Dockerfile, Kubernetes manifesti, CI workflow, Argo CD tanimi ve testler arasindaki gercek baglantilar tarandi.

Degerlendirme su sorular uzerinden yapildi:

- Bir verinin tek ve acik bir system of record sahibi var mi?
- Bir islem yarida kalirsa para ve durum nasil toparlaniyor?
- Database commit ile mesaj yayini atomik mi?
- Ayni istek veya mesaj tekrar gelirse sonuc ayni mi?
- Olaylar semali, surumlu ve geriye uyumlu mu?
- Servis kimligi ve kullanici kimligi her hop'ta dogrulaniyor mu?
- Kritik operasyonun bagimliliklari, toleransi, RTO ve RPO'su tanimli mi?
- Read model yeniden kurulabilir ve kaynakla mutabik hale getirilebilir mi?
- Kod sinirlari CI tarafindan uygulanabilir mi?
- Bir olay uctan uca trace, metric ve log ile izlenebilir mi?

Harici referans olarak BCBS operasyonel dayaniklilik ve risk verisi ilkeleri, NIST Zero Trust ve SSDF, OpenID FAPI 2.0, OWASP API Security, Kafka teslimat garantileri, Debezium Outbox, OpenTelemetry ve cift tarafli ledger garantileri kullanildi.

## 3. Mevcut Sistem Envanteri

### 3.1 Servisler

| Servis | Rol | Ana iletisim | Veri sahibi |
|---|---|---|---|
| gateway | Dis HTTP girisi, JWT kontrolu, routing | HTTP, Redis | Edge kurallari ve gecici kontrol verisi |
| user-service | Keycloak yonetimi, login/register, token decode | HTTP, gRPC, Kafka | Kimlik Keycloak'ta; uygulama yardimci kayitlari PostgreSQL/Redis'te |
| customer-service | Customer facade | HTTP, gRPC | Veri sahibi degil |
| customer-service-command | Customer write modeli | HTTP, gRPC, Kafka | PostgreSQL `banking_customer_command` |
| customer-service-query | Customer read/search modeli | HTTP, gRPC, Kafka | MongoDB ve Elasticsearch projection |
| money-service | Canli onboarding ve para hareketi | HTTP, Kafka | PostgreSQL `banking_money`, Redis idempotency |
| money-service-command | Ayri money CQRS write modeli | HTTP, Kafka | PostgreSQL `banking_money_command` |
| money-service-query | Ayri money CQRS read modeli | HTTP, gRPC, Kafka | MongoDB ve Elasticsearch projection |
| transaction-service | Islem orkestrasyonu ve durum gecmisi | HTTP, gRPC, Kafka | PostgreSQL `banking_transactions` |
| fraud-service | Transfer fraud asamasi | Kafka | PostgreSQL `banking_fraud` |
| admin-service | Operasyon ve admin facade | HTTP, gRPC, Kafka, Kubernetes API | Veri sahibi degil |
| admin-service-command | Admin history write modeli | gRPC, Kafka | PostgreSQL `banking_admin_command` |
| admin-service-query | Admin history read/search modeli | HTTP, gRPC, Kafka | MongoDB ve Elasticsearch projection |

### 3.2 Altyapi

- PostgreSQL tek bir Kubernetes deployment'i icinde birden fazla mantiksal database barindiriyor.
- MongoDB, Elasticsearch, Redis, Kafka, ZooKeeper ve Keycloak tek replica calisiyor.
- Kafka PLAINTEXT ve replication factor 1 ile tanimli.
- Elasticsearch single-node ve security kapali.
- Uygulamalarin cogu tek replica basliyor; bazi HPA tanimlari mevcut.
- Runtime secret degerleri manifestlerde veya guvensiz varsayilanlarda bulunuyor.
- Uygulama podlarina `pods/exec`, deployment scale ve HPA erisimi veren genis RBAC kurallari var.

### 3.3 Guclu temeller

- Java 21 ve guncel Spring yaklasimi.
- Para alanlarinda `BigDecimal` kullanimi.
- Customer BaseEntity icinde UUID, audit tarihleri, actor alanlari, `@Version` ve soft delete.
- Command ve query ayrimi ile MongoDB/Elasticsearch projection ornekleri.
- Kafka olay anahtarlarinin bazi akislarda aggregate/event kimligine baglanmasi.
- Bazi consumer'larda Redis veya database tabanli tekrar-isleme kontrolu.
- Multi-stage Docker build, Actuator health endpointleri, Gitleaks, JaCoCo ve Sonar altyapisi.
- Kullanici kaydinda Keycloak sonrasi customer gRPC onboarding ve hata halinde Keycloak rollback niyeti.

## 4. Mevcut Tam Veri Akisi

### 4.1 Dis istek ve kimlik akisi

1. Istemci `gateway:8095` uzerinden HTTP istegi gonderir.
2. Gateway Keycloak realm issuer bilgisiyle JWT dogrular.
3. Gateway route path'ine gore ilgili servise proxy yapar.
4. Kullanici bilgileri `X-User-*` header'lariyla ic servislere aktarilir.
5. Ic servislerin cogu bu header'lari yeniden kriptografik olarak dogrulamaz.
6. Transaction service ayrica Authorization header'ini user-service gRPC token decode endpointine yollar.

Sonuc: perimeter girisinde kimlik dogrulama var, fakat pod agina erisen bir aktor forward edilen header'lari taklit edebilir. NetworkPolicy ve servis kimligi olmadigi icin ic ag guven siniri kabul edilmektedir.

### 4.2 Kullanici kaydi ve ilk hesap acilisi

1. Istemci register istegini Gateway uzerinden user-service'e gonderir.
2. `UserAuthService`, `KeycloakAdminService.createUser` ile once Keycloak kullanicisini olusturur.
3. Keycloak kaydinda e-posta mevcut kodda dogrulama yapilmadan `emailVerified=true` olur.
4. User-service, customer-service `CustomerOnboardingGrpcEndpoint` metodunu gRPC ile cagirir.
5. Customer facade istegi customer-service-command gRPC endpointine iletir.
6. Customer command modeli PostgreSQL'e yazilir.
7. Customer projection olayi `banking-microservices.customer.projection-sync.v1` topic'ine gonderilir.
8. Customer query service olayi tuketir; MongoDB read modelini ve Elasticsearch indeksini gunceller.
9. User-service `banking-microservices.user.created.v1` olayini yayinlar.
10. Canli money-service bu olayi tuketip ilk para hesabini `banking_money` database'inde olusturur.
11. Sonuc `money.account.created.v1` veya `money.account.create-failed.v1` ile user-service'e bildirilir.

Mevcut compensation yalniz customer onboarding cagrisi hata verirse Keycloak kullanicisini siler. Customer kaydi basarili olduktan sonra `user.created` yayini asenkron olarak broker'da hata alirsa publisher bunu beklemedigi icin hata gorunmeyebilir. Gorulse bile genel catch Keycloak'i silerken customer kaydi kalabilir. Bu nedenle akis atomik degildir ve reconciliation gerektirir.

### 4.3 Login, refresh ve logout

1. User-service Keycloak token endpointiyle kullanici girisini yapar.
2. Access ve refresh token istemciye doner.
3. Refresh akisi Keycloak'a refresh token ile gider.
4. Logout refresh token/session iptaline dayanir.

Hedefte public/high-value API istemcileri icin FAPI 2.0 profili, Authorization Code + PKCE, sender-constrained token ve uygun confidential-client kimlik dogrulamasi degerlendirilmelidir. Password grant veya admin kullanici sifresiyle servis otomasyonu kullanilmamalidir.

### 4.4 Customer komut akisi

1. Istek customer-service facade'a gelir.
2. Create, profil, contact verification, status, KYC, risk, MFA, special customer ve soft delete komutlari command servisine gRPC ile gider.
3. Command service validasyon ve domain degisikligini yapar.
4. JPA repository authoritative PostgreSQL satirini kaydeder.
5. Ayni application akisi Kafka projection olayini yollar.
6. Query service olayi tuketir, once MongoDB sonra Elasticsearch yazar.

Riskler: DB ve Kafka atomik degildir; Mongo ve Elasticsearch de atomik degildir. Event sirasi `occurredAt` ile kontrol edilir; ayni timestamp veya gecikmis olaylarda aggregate sequence kadar guclu degildir.

### 4.5 Customer sorgu akisi

1. Istek facade'a gelir ve customer-service-query gRPC endpointine yonlenir.
2. Kimlik bazli tekil okuma MongoDB read modelinden yapilir.
3. Arama Elasticsearch uzerinden yapilir.
4. Read model gecikmesi nedeniyle write sonrasi anlik okuma eski veri donebilir.

API bu eventual consistency davranisini versiyon, ETag, projection lag veya `lastProcessedEvent` bilgisiyle aciklamamaktadir.

### 4.6 Deposit ve withdrawal akisi

1. Transaction service istegi ve token detaylarini alir.
2. Transaction kaydi `CREATED` olarak PostgreSQL'e yazilir.
3. `transaction.created.v1` yayinlanir.
4. Money-service olayi tuketir ve ilgili `money` alanini dogrudan artirir veya azaltir.
5. Basari `transaction.completed.v1`, hata `transaction.failed.v1` olarak yayinlanir.
6. Transaction service sonucu tuketip workflow durumunu gunceller.

Bu akis muhasebe kaydi degil bakiye mutasyonudur. Kaynak hesap, karsi hesap, currency, journal ve balanced entries olmadigi icin finansal kanit yeniden uretilemez.

### 4.7 Transfer akisi

1. Transaction service bir event UUID uretir, transaction kaydini yazar ve `transaction.created.v1` yayinlar.
2. Money-service sender bakiyesini `money` alanindan dusup `blockedMoney` alanina tasir.
3. Money-service `transaction.user-validation.request.v1` yayinlar.
4. User-service gonderen/alici kimliklerini kontrol edip `transaction.user-validation.success.v1` yayinlar.
5. Fraud-service bu olayi alir, mevcut uygulamada sinirli bir gate uygulayip `transaction.fraud.checked.v1` yayinlar.
6. Money-service bloke parayi sender'dan cikarir ve receiver bakiyesine ekler.
7. Money-service `transaction.completed.v1` yayinlar.
8. Transaction service workflow kaydini `COMPLETED` yapar.

Hata olayi birden fazla servisin ortak `transaction.failed.v1` topic'ine yazilmaktadir. Hangi asamanin, hangi hata semasiyla, retry edilip edilmediginin merkezi sozlesmesi yoktur.

### 4.8 Tamamlanmis transaction reversal/saga akisi

1. Kullanici veya admin cancellation endpointini cagirir.
2. Transaction service sahiplik/yetki kontrolu yapar.
3. Tamamlanmis islem icin SagaEvents kaydi olusturur.
4. `transaction.saga.created.v1` money-service'e gonderilir.
5. Money-service ters para hareketini yapar.
6. `saga.money.completed.v1` veya `saga.money.failed.v1` transaction-service'e doner.
7. Transaction service saga durumunu gunceller.

Inceleme sirasinda sender/listener topic'lerinin ters baglandigi bulundu ve duzeltildi. Kalan risk, transaction kaydinin financial compensation tamamlanmadan `REVERSED` yapilabilmesidir. Hedef durum `REVERSAL_PENDING`, `REVERSED` ve `REVERSAL_FAILED` seklinde acik state machine olmalidir.

### 4.9 Money CQRS projection akisi

1. money-service-command kendi `banking_money_command` write modelini gunceller.
2. `money.projection-sync.v1` olayini yayinlar ve send future sonucunu bekler.
3. money-service-query olayi tuketir.
4. MongoDB ve Elasticsearch read modellerini gunceller.

Bu akis canli transaction pipeline'indaki `money-service` degisikliklerini kapsamaz. Bu nedenle iki farkli account gercegi olusabilir. Bu en onemli bounded-context sahiplik sorunlarindan biridir.

### 4.10 Admin history akisi

1. Admin facade operasyonu alir.
2. History komutu Kafka ile admin-service-command'a gider veya gRPC command/query cagrilari yapilir.
3. Command service PostgreSQL'e yazar.
4. Projection sync olayi yayinlar.
5. Query service MongoDB ve Elasticsearch'i gunceller.

Bu kayitlar degistirilemez, hash-zincirli veya WORM saklamali bir denetim izi degildir. Bu nedenle regulator-grade audit log olarak kabul edilmemelidir.

## 5. Kritik Farklar ve Risk Kaydi

### 5.1 P0: Para sistemi ledger degil

`money-service` ve `money-service-command` hesap satirlarindaki `money` ve `blockedMoney` alanlarini mutasyona ugratiyor. Kurumsal hedefte her finansal hareket bir journal transaction ve en az iki entry olarak, currency bazinda toplam debit ile credit esit olacak bicimde atomik yazilmalidir.

Gerekli garantiler:

- Posted entry degistirilemez ve silinemez.
- Duzeltme, onceki kaydi update etmek yerine reversal entry ile yapilir.
- Her transaction currency bazinda balanced olmalidir.
- Tum entry'ler ya birlikte commit olur ya hicbiri olmaz.
- Idempotency key ayni islem sonucunu tekrar dondurur.
- Available, pending, blocked ve posted bakiyeler entry/hold toplamindan turetilir.
- Her bakiye versiyonu hangi entry'lerden olustugu ile kanitlanabilir.
- Gun sonu ve surekli mutabakat farklari otomatik alarm uretir.

### 5.2 P0: Money sahipligi ikiye bolunmus

Canli onboarding ve transaction akisi `money-service/banking_money` kullanirken, command/query servisleri `banking_money_command` ve projection depolarini kullaniyor. Ayni kavram icin iki write modeli kabul edilemez.

Karar: Yeni `ledger-service` veya yeniden adlandirilmis tek bir money command servisi canonical owner olmali. Mevcut money-service anti-corruption adapter olarak gecici kalmali; tum yazarlar kademeli olarak canonical posting API'sine tasinmali. Query servisi yalniz bu canonical olaylardan beslenmelidir.

### 5.3 P0: Database ve Kafka dual-write

Customer, transaction, admin ve money akislari once repository save edip sonra Kafka send yapiyor. Ikisi ayri commit oldugu icin su durumlar mumkundur:

- DB commit olur, olay kaybolur.
- Olay gider, local transaction rollback olur.
- Retry ayni finansal hareketi iki kez uygular.
- Consumer basarili islemden once offset commit eder veya hatayi loglayip yutar.

Hedef: Authoritative DB degisikligi ile outbox satiri ayni local transaction'da yazilmali. CDC/Debezium outbox router veya kontrollu outbox publisher mesaji Kafka'ya tasimali. Consumer inbox kaydi ve business write ayni transaction'da atomik olmali.

### 5.4 P0: Kayit orkestrasyonu tam toparlanabilir degil

Keycloak, customer ve money hesap acilisi uc farkli consistency boundary'dir. Senkron rollback tek basina yeterli degildir.

Hedef registration state machine:

- `IDENTITY_PENDING`
- `IDENTITY_CREATED`
- `CUSTOMER_PENDING`
- `CUSTOMER_CREATED`
- `ACCOUNT_PENDING`
- `ACTIVE`
- `COMPENSATION_PENDING`
- `FAILED_MANUAL_REVIEW`

Her adim idempotent command, outbox olayi, deadline ve reconciliation kaydina sahip olmalidir. Keycloak silme basarisizsa islem kaybolmamalı, operasyon kuyruğuna dusmelidir.

### 5.5 P0: Uretim altyapisi tek hata noktalariyla dolu

PostgreSQL, Redis, MongoDB, Elasticsearch, Kafka, ZooKeeper ve Keycloak tek replica. Kafka RF=1 ve PLAINTEXT. Bu konfigurasyon gelistirme icin kullanilabilir, uretim icin kullanilamaz.

Hedef minimumlar urun SLO'suna gore belirlenmelidir:

- Kafka en az uc broker, uygun replication/min ISR, rack/zone dagilimi ve TLS/SASL.
- PostgreSQL HA, otomatik failover, PITR, sifreli backup ve duzenli restore testi.
- Keycloak HA ve harici HA database.
- Redis Sentinel/Cluster veya yonetilen HA hizmeti.
- MongoDB replica set; Elasticsearch en az uygun master/data node topolojisi.
- Pod anti-affinity, topology spread, PDB, requests/limits, startup/readiness/liveness probe.
- Belgelenmis RPO, RTO ve bolge/zone kaybi tatbikati.

### 5.6 P1: Mesaj sozlesmeleri kopya ve semasiz

Ayni transaction DTO ve enumlari user, money, transaction ve fraud servislerinde kopyalanmis. Gson/manual JSON ve Spring type header ayarlari birlikte kullaniliyor. Schema Registry, AsyncAPI ve compatibility gate yok.

Hedef event envelope alanlari:

- `eventId`, `eventType`, `eventVersion`, `occurredAt` UTC Instant.
- `producer`, `aggregateType`, `aggregateId`, `aggregateVersion`.
- `correlationId`, `causationId`, `traceparent`.
- `partitionKey`, `tenantId` gerekiyorsa, veri siniflandirma etiketi.
- Surumlu payload ve acik sema referansi.

Avro veya Protobuf + Schema Registry tercih edilmelidir. JSON kalacaksa JSON Schema compatibility CI'da kontrol edilmelidir. Ortak bir devasa DTO kutuphanesi yerine contract artifact uretilmelidir.

### 5.7 P1: Kafka hata yonetimi eksik

Birden fazla publisher `kafkaTemplate.send` sonucunu beklemeden basari logluyor. Broker hatasi future tamamlanirken olusursa uygulama bunu kacirabilir. Listener'larda standart exponential backoff, retryable/non-retryable ayrimi, DLT, quarantine ve replay araci gorulmedi.

Hedef:

- Producer `acks=all`, idempotence, uygun delivery timeout ve callback/transaction semantics.
- Consumer manual/transactional offset stratejisi.
- Atomik inbox ve unique event constraint.
- Sinirli retry, jitter ve DLT.
- DLT mesajinda orijinal topic/partition/offset, exception code ve trace bilgisi.
- Yetkili, auditli replay araci.
- Consumer lag, oldest message age ve poison-message alarmlari.

Kafka idempotent producer tek basina end-to-end exactly-once saglamaz. DB yan etkisi olan consumer yine idempotent olmak zorundadir.

### 5.8 P1: Ic servis guveni ve secret yonetimi zayif

- Yalniz gateway JWT dogruluyor; servisler forward header'lara guveniyor.
- gRPC cagrilari plaintext.
- SSE token query parametresinden aliniyor.
- Keycloak admin kullanicisi ve sifresiyle `admin-cli` kullaniliyor.
- Manifestlerde veya uygulama varsayilanlarinda sifre/client secret bulunuyor.
- Elasticsearch security kapali.
- Genis Kubernetes RBAC application podlarina cluster operasyon yetkisi veriyor.

Hedef: servis kimligi, workload certificate, mTLS, token audience kontrolu, NetworkPolicy, least privilege service account, External Secrets/Vault/KMS, rotasyon ve merkezi audit. FAPI 2.0 kaynak sunucularinin query parametresinde access token kabul etmemesini ister; SSE cozumunde guvenli cookie veya tek kullanimlik cok kisa omurlu ticket kullanilmalidir.

### 5.9 P1: Migration ve veri modeli yonetimi eksik

`ddl-auto:update` ve elle calisan schema alter kodu uretim icin belirleyici migration gecmisi saglamaz. Flyway veya Liquibase ile immutable, sirali, geri-donus stratejili migration kullanilmali; runtime `ddl-auto=validate` olmalidir.

LocalDateTime ve sistem timezone kullanimi olay siralamasi ve cok bolgeli calismada belirsizlik yaratir. Saklama ve mesajlasmada UTC `Instant`, kullaniciya sunumda acik zone kullanilmalidir. Test edilebilirlik icin `Clock` inject edilmelidir.

### 5.10 P1: DTO ve API sinirlari tutarsiz

- Bazi controller'lar persistence/Keycloak modelini dogrudan donduruyor veya request olarak aliyor.
- Entity'lerde Lombok `@Data` lazy relation, equals/hashCode ve hassas toString riski olusturuyor.
- String ve UUID kimlikler karisik.
- Para modelinde ISO 4217 currency ve rounding kurali yok.
- Transaction kaydinda kapsamli token detaylari TEXT olarak saklaniyor.
- Tum DTO'yu loglayan kod PII ve auth claim sizintisi uretebilir.
- Error response semasi servisler arasinda ortak degil.

Hedefte persistence entity API sinirini gecmemeli. Problem cevaplari RFC 9457 `ProblemDetail` tabanli, stabil `errorCode`, `traceId`, `instance` ve guvenli ayrinti icermelidir.

### 5.11 P1: Concurrency ve finansal kilitleme modeli yetersiz

Customer modelinde `@Version` vardir; money modellerinde ayni garanti sistematik degildir. Atomic update query bazi yarislari azaltir fakat cift hesapli transferde kilit sirasi, deadlock retry, aggregate version ve posting atomikligi acik degildir.

Hedef:

- Tek ledger transaction icinde tum entry'ler.
- Deterministik hesap kilit sirasi veya serializable/uygun optimistic strategy.
- Version conflict icin sinirli retry.
- Negatif bakiye, limit ve overdraft kurallarinin ayni consistency boundary'de kontrolu.
- Ayni idempotency key + farkli payload durumunda conflict.
- Concurrency ve property-based invariant testleri.

### 5.12 P1: Fraud/KYC/AML yalniz model alanlari seviyesinde

Customer modelinde KYC/risk alanlari olmasi olumludur; ancak karar motoru, sanction/PEP screening, velocity/rule set, model version, evidence, case management, maker-checker ve manual review akislarina rastlanmadi. Fraud service su anda daha cok iletim asamasidir.

Bu alanlar tek bir `riskScore` integer'ina indirgenmemelidir. Karar; rule/model version, input snapshot, reason codes, decision, reviewer ve zaman bilgisiyle yeniden kanitlanabilir olmalidir.

### 5.13 P1: Gozlemlenebilirlik uctan uca degil

Actuator ve servis adi iceren log patternleri var, fakat trace/correlation standardi, metrics taxonomy ve servis haritasi yok.

Hedef:

- OpenTelemetry Java Agent ile HTTP, gRPC, Kafka, JDBC, Redis ve Elasticsearch trace'leri.
- OTLP Collector ve SigNoz.
- `trace_id`, `span_id`, `correlation_id`, `causation_id`, `event_id` log alanlari.
- RED metrikleri; ledger posting latency, failure, duplicate, reversal ve reconciliation farklari.
- Kafka consumer lag ve projection lag.
- Kritik is akisi bazli SLI/SLO ve error budget.
- PII redaction ve log retention politikasi.

### 5.14 P1: Test piramidi finansal riski kapsamiyor

Mevcut testler agirlikla Mockito unit testidir. Eksik katmanlar:

- Testcontainers ile PostgreSQL, Kafka, Redis, MongoDB ve Elasticsearch entegrasyonu.
- gRPC ve HTTP consumer-driven contract testleri.
- Schema compatibility ve proto breaking-change kontrolu.
- Flyway migration forward/rollback testi.
- Ayni hesaba eszamanli debit/credit ve duplicate event testleri.
- Ledger balanced invariant ve reversal property testleri.
- Kayit compensation, outbox relay ve DLT replay testleri.
- Yuk, soak, spike ve kapasite testleri.
- Pod/broker/database kaybi, network partition ve gecikme kaos testleri.
- Backup restore ve bolge failover tatbikati.

### 5.15 P2: CI/CD ve supply-chain eksikleri

CI matrisi daha once yalniz bes servisi kapsiyordu; bu calismada 13 servisin tamamini kapsayacak sekilde genisletildi. Kalan hedefler:

- Ortak parent POM veya version catalog ile dependency/plugin standardi.
- Dependabot/Renovate ve duzenli patch politikasi.
- SAST, dependency scan, container scan, IaC scan.
- CycloneDX SBOM.
- Cosign image signature ve provenance/SLSA metadata.
- Immutable image digest ile deploy.
- Environment promotion, approval ve rollback.
- Argo CD sync health, policy gate ve drift alarmi.
- Ephemeral integration environment.

Argo CD tanimi var olmayan `helm/springbank` yolunu gosterdigi icin deploy edilemiyordu; bu calismada mevcut `k8s` yoluna baglandi. Uzun vadede tekrar eden manifestler Kustomize veya gercek bir Helm chart ile ortam katmanlarina ayrilmalidir.

## 6. Hedef Bounded Context Haritasi

### 6.1 Identity and Access

Keycloak federation, authentication, session, client ve role/policy yonetimi. Customer profilinin sahibi olmamalidir. Admin credential yerine service account/private key tabanli client kullanmalidir.

### 6.2 Party and Customer

Gercek/tuzel kisi, iletisim, tercih, demographic alanlar ve identity baglantisi. Customer UUID ile Keycloak subject ayridir. Email identity username olmak zorunda degildir.

### 6.3 KYC/AML and Customer Risk

KYC case, belge, screening, decision, reason code, evidence ve inceleme akislarini sahiplenir. Customer context yalniz ozet durum ve referans tutar.

### 6.4 Product and Account

Hesap urunu, hesap yasam dongusu, sahiplik, currency, limit, durum ve IBAN/hesap numarasi tahsisi. Bakiye gerceginin sahibi degil; ledger account referansina sahiptir.

### 6.5 Ledger

Journal, ledger account, posting, entry, hold ve balance snapshot sahibidir. Finansal system of record budur. En guclu tutarlilik ve audit garantileri burada uygulanir.

### 6.6 Payments and Transfer Orchestration

Kullanici niyetini, beneficiary, limit/risk adimlarini, state machine ve external rail entegrasyonunu yonetir. Parayi dogrudan update etmez; ledger'a posting command gonderir.

### 6.7 Fraud and Financial Crime

Gercek zamanli karar, velocity, cihaz/oturum sinyali, model/rule version ve case escalation sahibidir. Karar sonucu reason code ile doner.

### 6.8 Fees, Limits and Pricing

Ucret, vergi, gunluk/aylik limit, segment ve urun kurallarini merkezi fakat domain-uygun sekilde yonetir. Ledger posting icine ek entry'ler olarak yansir.

### 6.9 Reconciliation and Settlement

Internal ledger, payment rail, banka hesap ekstresi ve projection verisini karsilastirir. Farklari otomatik case olarak acar; elle SQL ile duzeltmez.

### 6.10 Notification

Email/SMS/push teslimatini asenkron yapar. Finansal transaction basarisi notification basarisina bagli olmamalidir.

### 6.11 Audit and Compliance Evidence

Admin ve kritik islem audit olaylarini append-only, erisim kontrollu ve retention/legal-hold kurallarina uygun saklar. Operational log ile audit log ayni sey degildir.

Her bounded context hemen ayri deployable servis olmak zorunda degildir. DDD siniri once kod ve veri sahipligini belirler; bagimsiz olcek, release veya guvenlik gereksinimi varsa fiziksel servis ayrilir.

## 7. Hedef Uygulama Mimarisi

Her servis icin onerilen hexagonal katmanlar:

- `api`: HTTP/gRPC request-response DTO, validation ve protocol mapping.
- `application`: use case, command/query handler, transaction boundary ve authorization policy.
- `domain`: aggregate, value object, invariant, domain service ve domain event.
- `ports`: repository, event publisher, clock, identity/risk/ledger gateway arayuzleri.
- `adapters`: JPA, Kafka, gRPC client/server, Keycloak, Redis ve Elasticsearch implementasyonlari.
- `bootstrap`: Spring configuration ve runtime wiring.

Kurallar:

- Domain Spring/JPA/Kafka siniflarina bagimli olmamali.
- Controller repository cagirmaz.
- Controller persistence entity dondurmez.
- Mapper domain kurallarini barindirmamali.
- Transaction siniri application use case seviyesinde olmali.
- Cross-service cagri bir port arkasinda olmali.
- Her outbound port timeout ve hata taxonomy'si tanimlamali.

## 8. Ledger Hedef Veri Modeli

### 8.1 Temel varliklar

| Varlik | Amac | Temel kural |
|---|---|---|
| Ledger | Muhasebe izolasyon siniri | Farkli ledger hesaplari ayni transaction'da karistirilmaz |
| LedgerAccount | Degerin tutuldugu hesap | Currency ve normal balance aciktir |
| JournalTransaction | Tek finansal niyet | Tum entry'ler atomik ve balanced |
| Entry | Debit veya credit satiri | Posted olduktan sonra immutable |
| Hold | Kullanilabilir bakiyeden rezervasyon | Expiry, capture ve release idempotent |
| BalanceSnapshot | Performans icin turetilmis ozet | Entry toplamindan yeniden uretilebilir |
| IdempotencyRecord | Tekrar istek korumasi | Key, request hash, status ve response tutulur |
| OutboxEvent | Commit edilmis domain olayi | Journal write ile ayni transaction |
| InboxEvent | Tuketilen olay kaydi | Event ID unique ve yan etkiyle atomik |
| ReconciliationRun | Mutabakat calismasi | Kaynak, pencere, fark ve sonuc kanitli |

### 8.2 Para deger nesnesi

Bir para alani yalniz `BigDecimal` olmamalidir. Amount ile ISO 4217 currency birlikte tasinmali; scale ve rounding currency metadata'dan gelmelidir. Float/double yasaklanmalidir. API'de decimal string veya minor-unit integer stratejisinden biri sozlesme olarak secilmelidir.

### 8.3 Posting ornegi mantigi

Bir kullanicidan digerine 100 TRY transfer tek bir journal transaction icinde en az sender debit ve receiver credit entry'leri uretir. Ucret varsa fee income ve ilgili kullanici entry'si ayni balanced transaction'a eklenir. Hata halinde satirlar update edilmez; posting olusmaz veya ters posting yazilir.

### 8.4 IBAN

Rastgele `TR` + karakter dizisi gecerli IBAN degildir. Ulke uzunlugu, BBAN yapisi, kurum kodu ve MOD-97 checksum dogrulanmalidir. Gercek banka lisansi/kurum kodu yoksa sistem bunu IBAN olarak adlandirmamali; `internalAccountNumber` kullanmalidir.

## 9. Pattern Kararlari

### 9.1 Kullanilacak patternler

| Pattern | Nerede | Neden |
|---|---|---|
| DDD bounded context | Tum domainler | Veri sahipligini ve dili netlestirmek |
| Hexagonal architecture | Her servis | Framework ile domaini ayirmak |
| CQRS | Customer search, transaction reporting, audit | Okuma ihtiyaci write modelinden gercekten farkli |
| Transactional Outbox | DB degisikligi + event | Dual-write kaybini onlemek |
| Transactional Inbox | Kafka consumer yan etkisi | Duplicate islemeyi guvenli yapmak |
| Saga orchestration/state machine | Registration, transfer, reversal | Uzun sureli adimlari ve compensation'i gorunur yapmak |
| Idempotency Key | Tum finansal POST/command | Retry'nin cift finansal etki yapmamasini saglamak |
| Anti-Corruption Layer | Legacy money -> ledger gecisi | Eski modeli hedef domain dilinden izole etmek |
| Strangler migration | Money sahipligi birlestirme | Big-bang gecis riskini azaltmak |
| Circuit breaker/bulkhead/timeout | Senkron dis bagimliliklar | Zincirleme arizayi sinirlamak |
| Materialized view | Arama ve raporlama | Ledger/write DB uzerindeki sorgu yukunu azaltmak |
| Reconciliation | Ledger, external rail, projection | Sessiz veri farkini bulmak |
| Append-only audit | Kritik admin/finans olaylari | Kanit ve degismezlik |

### 9.2 Sinirli kullanilacak patternler

- Event sourcing tum domainlere uygulanmamalidir. Ledger journal dogasi geregi append-only olabilir; customer profilinin her alanini event source etmek gereksiz karmasikliktir.
- CQRS her CRUD servisinde zorunlu degildir. Ayrik olcek, query modeli veya guvenlik ihtiyaci yoksa tek servis ve tek database daha dogrudur.
- Service mesh ancak servis kimligi, policy ve operasyon ekibi hazir oldugunda eklenmelidir. Once NetworkPolicy, TLS ve workload identity temeli kurulmalidir.
- Cache finansal gercegin sahibi olmamalidir. Bakiye cache'i stale davranisi ve invalidation sozlesmesi olmadan kullanilmamalidir.
- Kafka transaction, relational database ile tek atomik transaction saglamaz; outbox ihtiyacini ortadan kaldirmaz.

## 10. DTO ve Sozlesme Standardi

### 10.1 DTO siniflari

| Tur | Adlandirma | Kural |
|---|---|---|
| HTTP request | `CreateTransferRequest` | Immutable, validation annotation, domain/entity yok |
| HTTP response | `TransferResponse` | Hassas alan whitelist, stabil API semasi |
| Application command | `CreateTransferCommand` | Actor/context dahil, transporttan bagimsiz |
| Application query | `GetCustomerQuery` | Pagination/sort/filter sinirli |
| Domain value | `Money`, `CustomerId`, `LedgerAccountId` | Invariant constructor'da |
| Persistence entity | `LedgerEntryEntity` | API ve event disina cikmaz |
| Integration event | `TransferCompletedV1` | Surumlu, registry-backed, immutable |
| gRPC message | Proto request/response | Buf breaking check ve deadline politikasi |
| Error | RFC 9457 ProblemDetail | Stabil error code, trace ID, guvenli mesaj |

### 10.2 Mapping kurallari

- API request -> application command controller adapter'da.
- Command -> domain value object application katmaninda.
- Domain -> entity repository adapter'da.
- Domain event -> integration event outbox adapter'da.
- MapStruct kullanilabilir; domain karari mapper'a gomulmez.
- Null/default degerleri transporttan domain'e sessizce gecirilmez.
- Enum degisiklikleri geriye uyumluluk plani olmadan event contract'a eklenmez veya yeniden adlandirilmaz.

### 10.3 Kimlik ve zaman

- Internal domain kimlikleri UUID value object olarak tutulmali.
- Keycloak subject ayri `IdentityId` olarak modellenmeli.
- Event ve persistence zamani UTC `Instant` olmali.
- Dogum tarihi zaman degil `LocalDate` olmalidir.
- Dis API'de ISO-8601 ve timezone semantigi acik olmalidir.
- `createdBy` gibi alanlar istemciden guvenilmeden authenticated principal'dan uretilmelidir.

### 10.4 Hassas veri

- Password, access token, refresh token, tam JWT claims/header, CVV ve secret kalici modele veya loga yazilmaz.
- Email, telefon, ad ve IBAN loglarda maskelenir veya hashlenir.
- DTO `toString` ciktisi loglanmaz; alan whitelist'i kullanilir.
- Encryption at rest, field-level encryption/tokenization ve anahtar rotasyonu veri sinifina gore uygulanir.

## 11. API ve gRPC Hedefleri

### 11.1 HTTP

- OpenAPI her public/internal HTTP servis icin uretilmeli.
- API version path veya header stratejisi tek olmalı.
- POST money operations `Idempotency-Key` istemeli.
- Pagination cursor tabanli ve maksimum limitli olmali.
- BOLA/BFLA icin her resource erisiminde subject-owner/policy kontrolu olmali.
- ETag/If-Match optimistic concurrency gerektiren resource'larda kullanilmali.
- Rate limit IP yerine client, user ve kritik business flow boyutunda uygulanmali.

### 11.2 gRPC

- Her client deadline koymali; sinirsiz bekleme olmamali.
- Retry yalniz idempotent method ve retryable status icin yapilmali.
- gRPC status ile domain error mapping merkezi standarda baglanmali.
- Proto package ve message isimleri surumlu olmali.
- Buf lint ve breaking-change CI gate kullanilmali.
- mTLS ve servis kimligi olmadan plaintext production cagrisi olmamali.
- Correlation ve trace context metadata ile tasinmali.

## 12. Guvenlik Hedef Mimarisi

### 12.1 Edge

- WAF/API gateway request size, rate, bot ve abuse kontrolu.
- OAuth/OIDC issuer, audience, scope ve role policy.
- FAPI 2.0 gerektiren high-value kanallarda PAR, PKCE ve mTLS/DPoP.
- Access token URL query parametresine konmamalı.
- Actuator detaylari public olmamali; health ayrintisi yetkili operasyon kanalinda kalmali.

### 12.2 Service-to-service

- Her workload benzersiz servis kimligine sahip olmali.
- mTLS ile hem client hem server dogrulanmali.
- User token propagation gerekiyorsa audience daraltilmis token exchange kullanilmali.
- NetworkPolicy default-deny olmali.
- Egress allowlist ve DNS/metadata endpoint korumasi uygulanmali.

### 12.3 Secret ve anahtar

- Git/Kubernetes duz YAML icinde secret tutulmamali.
- External Secrets Operator + Vault/KMS veya bulut secret manager kullanilmali.
- Her servis ayri DB role ve minimum yetki almali.
- Secret rotasyonu uygulama restart'ina bagli olmamali veya planli rollout yapmali.
- TLS/at-rest encryption anahtarlari ayrilmali ve audit edilmelidir.

### 12.4 Admin ve operasyon

- Maker-checker/four-eyes kritik admin islemlerinde zorunlu.
- Admin UI ile cluster operator yetkileri ayni serviste olmamali.
- `pods/exec` yalniz break-glass, zaman sinirli ve kayitli erisim olmali.
- Backup/restore veya scale islemleri operator/job ve onayli runbook ile calismali.

## 13. Dayaniklilik ve High-Scale Tasarim

High scale, sadece HPA veya Kafka kullanmak degildir. Her kritik akisin kapasite modeli ve bozulma davranisi tanimlanmalidir.

### 13.1 Kritik operasyon siniflari

| Operasyon | Tutarlilik | Ornek hedef |
|---|---|---|
| Ledger posting | Guclu, atomik | Kayip ve cift posting sifir; dusuk p99 |
| Transfer orkestrasyonu | Eventual state machine | Belirli surede terminal state veya manual review |
| Customer profil update | Guclu write, eventual search | Read-your-write primary query ile |
| Search/reporting | Eventual | Olculebilir projection lag |
| Notification | At-least-once + idempotent | Finansal sonucu bloke etmez |
| Audit | Append-only | Kayipsiz, erisim kontrollu retention |

Sayisal SLO degerleri trafik, is etkisi ve altyapi butcesi olmadan uydurulmamali. Her SLO icin olcum kaynagi, pencere, error budget ve escalation sahibi tanimlanmalidir.

### 13.2 Partition ve olcekleme

- Kafka partition key finansal siralama gereken aggregate, genellikle account/transaction ID olmalidir.
- Hot account ve celebrity-key senaryolari test edilmelidir.
- Database sharding ancak tek cluster kapasitesi kanitli olarak yetmediginde eklenmelidir.
- Ledger shard siniri transfer atomikligini bozmayacak sekilde secilmelidir.
- Read projection bagimsiz olceklenebilir; authoritative write yolu arama indeksine bagimli olmamalidir.
- Backpressure, queue depth ve load shedding kurallari bulunmalidir.

### 13.3 Ariza modlari

Her akis icin su enjeksiyonlar test edilmelidir: Kafka yok, DB failover, timeout, duplicate event, out-of-order event, partial projection, Keycloak yok, Elasticsearch yok, Redis yok, pod kill, zone loss, clock skew ve disk full.

## 14. Veri Yonetisimi ve BCBS 239 Perspektifi

- Her kritik veri elemaninin owner, source, lineage, kalite kurali ve retention'i olmalidir.
- Customer, account, ledger, transaction ve risk terimleri business glossary'de tanimlanmalidir.
- Risk ve finans raporlari kaynak entry'ye kadar lineage sunmalidir.
- Data quality kontrolleri completeness, accuracy, timeliness, uniqueness ve reconciliation kapsamalidir.
- Manuel duzeltmeler onay, reason, once/sonra ve actor ile audit edilmelidir.
- Projection ve warehouse verisi authoritative kaynak gibi kullanilmamalidir.
- PII erisimi amac sinirli, role/policy kontrollu ve kayitli olmalidir.

## 15. Mimari Zeka ve Yasayan Dokumantasyon

Secilen arac seti:

1. **jQAssistant + Neo4j:** Derlenmis Java yapisini grafa cevirir; controller-repository, katman sizintisi ve sistem saatine dogrudan bagimlilik gibi kurallari CI'da kontrol eder.
2. **EventCatalog:** Domain, servis, event ve business flow katalogudur. Bu calismada 13 servis, 17 Kafka olayi ve 6 kritik akis eklendi.
3. **OpenAPI + SpringDoc:** HTTP sozlesmesinin executable kaynagi.
4. **AsyncAPI + Springwolf:** Kafka producer/consumer ve channel sozlesmesi. Producer'lar annotation/config ile acik tanimlanmalidir.
5. **Protobuf descriptors + Buf:** gRPC sozlesme envanteri ve breaking-change kontrolu.
6. **OpenTelemetry + SigNoz:** Calisma zamani servis haritasi, trace, metric ve log korelasyonu.
7. **AppMap:** Lokal ve test ortaminda method/SQL/HTTP seviyesinde davranis izleri.
8. **Draw.io:** Stakeholder sunumu ve review snapshot'i; canonical kaynak degil.

Bu katmanlar birbirinin alternatifi degildir. jQAssistant kodun statik gercegini, EventCatalog niyet ve sahipligi, OpenTelemetry gercek runtime baglantilarini gosterir. Farklar drift olarak raporlanmalidir.

## 16. Neler Olmamali

- Ayni business verisi icin iki authoritative write service olmamali.
- Para bakiyesi controller/service icinde dogrudan `setBalance` ile degistirilmemeli.
- Posted finansal kayit update veya delete edilmemeli.
- DB commit ve Kafka send iki bagimsiz basari varsayimiyla birakilmamali.
- Consumer exception'i loglayip offset ilerletmemeli.
- “Kafka exactly once” ifadesi DB yan etkilerini de garanti ediyormus gibi kullanilmamali.
- Entity request/response DTO olarak kullanilmamali.
- Tek, her seyi tasiyan `TransactionDto` servisler arasinda kopyalanmamali.
- Access token query parametresinde, database'de veya application logunda bulunmamali.
- Internal network otomatik guvenli kabul edilmemeli.
- Kubernetes manifestine secret yazilmamali.
- Application poduna rutin olarak `pods/exec` ve cluster scale yetkisi verilmemeli.
- `ddl-auto=update` production schema yonetimi olmamali.
- Elasticsearch/MongoDB projection authoritative finansal kaynak olmamali.
- Her CRUD icin uc mikroservis acilmamali.
- Tum domain event sourced yapilmamali.
- Service mesh, multi-region veya sharding olculmus ihtiyac ve operasyon kabiliyeti olmadan eklenmemeli.
- Sadece mutlu yol unit testiyle finansal akis production'a alinmamali.
- E-posta gercek dogrulama olmadan verified sayilmamali.
- “special customer” tek boolean ile sinirsiz yetki veya limit bypass etmemeli.
- Risk karari aciklanamaz tek bir score'a indirgenmemeli.

## 17. Oncelikli Donusum Yol Haritasi

### Faz 0: Hemen, 0-30 gun

- Money system of record kararini ADR ile kesinlestir.
- Yeni money endpoint/ozelliklerini cakisan servislere eklemeyi dondur.
- Saga topic baglanti testini ve mevcut duzeltmeyi CI'a al.
- Tum Kafka send future'larini dogru ele al; kritik producer basarisini yanlis loglama.
- Secret'lari Git/Kubernetes duz metninden cikar ve rotate et.
- SSE query-token cozumunu kaldir.
- Transaction token detaylarini persistence/event/logdan cikar.
- `emailVerified=true` otomatik davranisini kaldir.
- Tum servisleri CI matrisi ve temel smoke test kapsaminda tut.
- EventCatalog katalog lint/build'ini CI'a ekle.
- Uretim benzeri ortamda backup ve restore testi yap.

### Faz 1: 30-90 gun

- Canonical ledger modelini ve posting API'sini kur.
- Flyway/Liquibase gecisi yap; `ddl-auto=validate` kullan.
- Customer, transaction, admin ve ledger icin outbox/inbox uygula.
- Schema Registry + AsyncAPI ve event envelope standardini devreye al.
- Kafka retry/DLT/replay standardini kur.
- OpenAPI ve Buf contract gate ekle.
- OpenTelemetry Agent + Collector + SigNoz kur.
- NetworkPolicy, mTLS/workload identity ve least privilege DB rollerini kur.
- Registration state machine ve reconciliation job olustur.

### Faz 2: 3-6 ay

- Legacy money yazimlarini ledger posting'e strangler ile tasi.
- Money CQRS read modelini yalniz canonical ledger/account olaylarindan rebuild et.
- Fraud/KYC case ve karar kaniti modelini gelistir.
- Limit, fee ve hold domainlerini ledger ile butunlestir.
- Testcontainers, contract, concurrency, property ve load test suite'i kur.
- HA Kafka/PostgreSQL/Keycloak ve zone dagilimini tamamla.
- SLO, alert, runbook ve on-call escalation kur.

### Faz 3: 6-12 ay

- Internal/external reconciliation ve settlement akislarini tamamla.
- Multi-region ihtiyacini RPO/RTO ve latency verisiyle karara bagla.
- DR ve bolge kaybi tatbikatlarini duzenli hale getir.
- BCBS 239 veri lineage, quality ve risk reporting kontrollerini olgunlastir.
- Supply-chain SBOM, signature, provenance ve policy-as-code gate'lerini zorunlu yap.
- Kapasite ve maliyet verisine gore partition/sharding stratejisini uygula.

## 18. Kabul Kriterleri

Sistem “kurumsal bankacilik seviyesine yaklasiyor” denmeden once en az su kanitlar bulunmalidir:

- Her finansal transaction currency bazinda balanced ve atomic entry setidir.
- Posted kayit update/delete edilemez; reversal ile duzeltilir.
- Duplicate API istegi ve duplicate Kafka olayi finansal etkiyi tekrarlamaz.
- DB commit sonrasi olay kaybi outbox ile engellenir.
- Tum projection'lar sifirdan rebuild edilebilir ve mutabakat raporu verir.
- Registration ve reversal yarida kalinca otomatik devam veya manual-review kaydi vardir.
- Servisler birbirini mTLS/workload identity ile dogrular.
- Uretim secret'i source control veya ConfigMap icinde degildir.
- Kafka broker/zone ve database failover testleri tanimli RTO/RPO'yu karsilar.
- Her kritik akisin trace'i, SLI'i, SLO'su ve sahibi vardir.
- Contract breaking change CI tarafindan engellenir.
- Backup restore, DLT replay ve reconciliation runbook'lari test edilmistir.
- Para concurrency/property testleri bakiye ve double-entry invariantlarini kanitlar.

## 19. Bu Calismada Uygulanan Degisiklikler

- Mevcut 13 servis, 17 Kafka olayi ve 6 kritik akis icin EventCatalog projesi olusturuldu.
- EventCatalog lint ve statik build kontrolu CI kalite kapisina eklendi.
- jQAssistant tarama konfigurasyonu, ilk katman kurallari ve Neo4j sorgulari eklendi. Gercek bytecode taramasi 3 kritik controller-repository bagimliligi ve 3 controller-persistence model sizintisi buldu.
- jQAssistant/XO'nun Turkce locale altinda boolean getter bulamama sorunu analiz scriptinde deterministik JVM locale ayariyla giderildi.
- CI servis matrisi 5 servisten 13 servise genisletildi.
- Argo CD'nin olmayan Helm yoluna baglantisi mevcut `k8s` manifest yoluna duzeltildi.
- Transaction reversal saga sender/listener topic eslesmesi duzeltildi.
- Mevcut Draw.io diyagrami dort sayfada sistem, registration, transaction/saga ve CQRS/admin akislarini belgelemeye devam ediyor.

Bu degisiklikler mimariyi gorunur ve denetlenebilir yapar; ledger/outbox/security donusumunun tamamlandigi anlamina gelmez.

## 20. Dis Referanslar

- BCBS Operational Resilience: https://www.bis.org/committees/bcbs/basel-consolidated-guidelines/module/orr/20
- BCBS Principles for Operational Resilience: https://www.bis.org/bcbs/publ/d516.pdf
- BCBS 239: https://www.bis.org/publ/bcbs239.pdf
- NIST SP 800-207 Zero Trust: https://csrc.nist.gov/pubs/sp/800/207/final
- NIST SP 800-207A Cloud-Native Zero Trust: https://csrc.nist.gov/pubs/sp/800/207/a/final
- NIST SSDF: https://csrc.nist.gov/pubs/sp/800/218/final
- OpenID FAPI 2.0 Security Profile: https://openid.net/specs/fapi-security-profile-2_0.html
- OWASP API Security Top 10 2023: https://api-security.owasp.org/editions/2023/en/0x00-header/
- RFC 9457 Problem Details: https://datatracker.ietf.org/doc/rfc9457/
- Debezium Outbox Event Router: https://debezium.io/documentation/reference/transformations/outbox-event-router.html
- Apache Kafka Design and Delivery Semantics: https://kafka.apache.org/40/design/design/
- OpenTelemetry Java Agent: https://opentelemetry.io/docs/zero-code/java/agent/
- jQAssistant Manual: https://jqassistant.github.io/jqassistant/current/
- EventCatalog: https://github.com/event-catalog/eventcatalog
- Modern Treasury Ledger Guarantees: https://docs.moderntreasury.com/ledgers/docs/ledgers-guarantees
