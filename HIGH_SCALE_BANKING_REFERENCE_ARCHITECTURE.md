# High-Scale Banking Tam Referans Mimarisi ve Veri Akislari

## 1. Belgenin Amaci ve Siniri

Bu belge, gercek hayatta yuksek hacimli bir perakende ve ticari bankanin dijital
kanallarini, cekirdek bankacilik kabiliyetlerini, para hareketlerini, risk ve
uyum fonksiyonlarini, veri platformunu ve operasyon altyapisini birlikte ele alan
hedef referans mimaridir.

Bu belge bir urun katalogu veya yalnizca mikroservis listesi degildir. Asagidaki
sorulara sistematik cevap verir:

- Bankada hangi is kabiliyetleri bulunmalidir?
- Hangi servis hangi verinin tek yazma sahibi olmalidir?
- Bir istek kanaldan girdikten sonra hangi kontrol ve kayitlardan gecmelidir?
- Para ne zaman rezerve edilir, ne zaman deftere islenir, ne zaman kesinlesir?
- Senkron API, gRPC, event, batch ve dosya aktarimi nerede kullanilmalidir?
- Hata, tekrar, gecikme, bolge kaybi ve dis sistem kesintisi nasil yonetilmelidir?
- Hangi veri guclu tutarli, hangisi eventual consistent olabilir?
- Denetci bir islemin kim tarafindan, hangi kararla ve hangi veriyle yapildigini
  nasil kanitlayabilir?
- Sistem nasil guvenli, olceklenebilir, geri yuklenebilir ve degistirilebilir kalir?

Hicbir tek dokuman dunyadaki her bankanin yuzde yuz ayni mimarisini tarif edemez.
Urunler, lisanslar, ulusal odeme raylari, muhasebe plani, veri yerelligi ve
regulasyon ulkeye ve bankaya gore degisir. Buradaki yapi; BIAN, ISO 20022, Basel,
CPMI, FATF, PCI, NIST ve modern dagitik sistem pratiklerini birlestiren kapsamli
bir baseline'dir. Her uygulamada hukuk, risk, finans, operasyon ve bilgi
guvenligi ekiplerinin resmi onayi gerekir.

## 2. Temel Mimari Ilkeler

### 2.1 Para dogrulugu performanstan once gelir

Bir banka sistemi hizli fakat yanlis bakiye uretemez. Finansal cekirdekte temel
invariantlar sunlardir:

- Her journal islemi dengelidir; toplam borc ve alacak ayni para biriminde
  sifirdir.
- Kaydedilmis finansal hareket degistirilmez veya silinmez; duzeltme ters kayit
  ve yeni kayitla yapilir.
- Ayni business talebi idempotency anahtariyla en fazla bir ekonomik sonuc
  dogurur.
- Hesap bakiyesi, ledger posting'lerinden turetilebilir ve mutabakatla
  kanitlanabilir.
- Para birimi, olcek, rounding, value date ve booking date acik alanlardir.
- Bir odemenin kabul edilmesi, muhasebelesmesi, clearing'i ve settlement'i ayni
  durum degildir.
- Dis sistem cevabi kaybolsa bile banka islemin gercek durumunu sorgulayabilir
  ve tekrar uzlastirabilir.

### 2.2 Veri sahipligi tektir

Her kritik kavramin bir system of record sahibi olur. Diger servisler kopya veya
projection tutabilir fakat authoritative kaydi degistiremez.

- Kimlik bilgisi: Identity and Access.
- Musteri/party profili: Party and Customer.
- KYC karari: KYC/AML.
- Urun kurali ve versiyonu: Product Catalog.
- Hesap yasam dongusu: Account.
- Finansal hareket ve bakiye: Ledger.
- Odeme durum makinesi: Payment Orchestration.
- Kart authorization/clearing: Card Processing.
- Kredi sozlesmesi ve schedule: Lending.
- Muhasebe hesap plani ve finansal raporlama: Finance/General Ledger.
- Denetim kaniti: Immutable Audit/Evidence Store.

Ortak database, ortak entity ve bir servisin diger servisin tablosuna yazmasi
yasaktir. Raporlama icin CDC/projection kullanilir; operasyonel veritabanina
cross-service join yapilmaz.

### 2.3 Guclu tutarlilik sadece gerekli sinirda kullanilir

- Ledger posting, hold yakalama/bosaltma, limit tuketimi ve tekil idempotency
  karari ayni transactional sinirda guclu tutarlidir.
- Bildirim, arama indeksi, musteri 360 gorunumu ve analitik projection eventual
  consistent olabilir.
- Banka genelinde tek dagitik ACID transaction kurulmaz. Yerel transaction,
  transactional outbox, idempotent consumer ve saga kullanilir.
- "Exactly once" broker ozelligi tek basina ekonomik exactly-once saglamaz.
  Business anahtari, unique constraint, inbox ve sonuc kaydi gerekir.

### 2.4 Kritik yol kisa, acik ve geri kazanilabilir olmalidir

Gercek zamanli transferin kritik yoluna e-posta, raporlama, data lake veya uzun
suren insan onayi eklenmez. Kritik yol yalnizca kimlik, yetki, limit, fraud,
sanctions gereksinimi, hesap/ledger ve odeme rayina cikis gibi karari degistiren
adimlari icerir. Yan etkiler event ile ayrilir.

### 2.5 Mikroservis hedef degil, sinirlandirma aracidir

Her tablo icin servis acilmaz. Servis siniri su unsurlardan en az birkacini
tasimalidir:

- Ayri is kabiliyeti ve terminoloji.
- Ayri veri sahipligi ve transaction siniri.
- Ayri olcekleme veya erisilebilirlik ihtiyaci.
- Ayri guvenlik/uyum siniri.
- Ayri ekip sahipligi ve degisim hizi.
- Bagimsiz deployment'in gercek faydasi.

Ledger gibi guclu invariantli bir alan gereksiz parcalanmaz. Musteri arama
projection'i gibi farkli okuma ihtiyaci ayri olceklenebilir.

### 2.6 Denetlenebilirlik sonradan eklenen log degildir

Her karar icin aktor, istemci, kanal, cihaz, correlation ID, causation ID,
policy/model versiyonu, onceki/yeni durum, zaman ve gerekce kaniti tutulur.
Parola, PIN, CVV, access token ve tam hassas veri loglanmaz. Audit log ile teknik
debug log ayridir; audit kaydi degistirilemez saklamaya aktarilir.

## 3. Referans Kritik Operasyon Siniflari

SLO, RTO ve RPO teknik ekibin tek basina sececegi sayilar degildir. Yonetim,
business owner, risk ve operasyon her kritik operasyon icin disruption tolerance
onaylar. Asagidaki degerler kapasite calismasinda baslangic ornegidir.

| Sinif | Ornek operasyonlar | Ornek erisilebilirlik | Ornek RTO | Ornek RPO |
|---|---|---:|---:|---:|
| Tier 0 | Ledger posting, kart authorization, instant payment, IAM | 99.99-99.999 | 5-15 dk | Finansal kayitta sifir veri kaybi hedefi |
| Tier 1 | Hesap sorgu, onboarding, fraud/AML karar, limit | 99.95-99.99 | 15-60 dk | Saniye-dakika; kaynaktan replay edilebilir |
| Tier 2 | Statement, bildirim, operasyon ekranlari | 99.9-99.95 | 1-4 saat | Event replay veya son checkpoint |
| Tier 3 | Analitik, kampanya, gecikmeli rapor | Is takvimine gore | 4-24 saat | Son basarili batch/checkpoint |

Availability tek basina yeterli degildir. Her akis icin su SLI'lar izlenir:

- Basari orani ve business decline orani ayri.
- Teknik hata ve timeout orani.
- p50, p95, p99 ve p99.9 latency.
- Kuyruk yasi ve consumer lag.
- Ledger-reconciliation fark adedi ve tutari.
- Bekleyen/kararsiz islem yasi.
- Fraud/AML decision latency ve fallback orani.
- Projection freshness.
- Settlement ve dosya teslim son saat uyumu.
- RTO/RPO tatbikat sonucu.

## 4. Ust Seviye Mantiksal Mimari

### 4.1 Kanal katmani

- Mobil bankacilik.
- Internet bankaciligi.
- Kurumsal bankacilik ve host-to-host.
- Subeler ve cagri merkezi.
- ATM, POS ve kiosk.
- Acik bankacilik TPP API'leri.
- Partner/embedded finance API'leri.
- Operasyon, fraud, AML ve back-office ekranlari.

Kanal katmani finansal gercegin sahibi degildir. BFF veya experience API kanal
formatini domain komutlarina cevirir; bakiye hesaplamaz ve kalici business karar
tutmaz.

### 4.2 Edge ve erisim katmani

- Global DNS, DDoS koruma ve CDN.
- WAF, bot/device risk ve API gateway.
- OAuth/OIDC authorization server.
- FAPI profili, mTLS veya DPoP ile sender-constrained token.
- Rate limit, quota, schema ve payload boyut kontrolu.
- Idempotency anahtari ve correlation ID kabul/uretimi.
- Kanal bazli routing, canary ve kill switch.
- PII redaction ve guvenli access log.

Gateway business ownership tasimaz. Yalnizca kaba yetkiyi uygular; hesap
sahipligi ve islem yetkisi resource service tarafinda tekrar dogrulanir.

### 4.3 Business capability katmani

Bu katman bounded context'lerden olusur:

1. Identity and Access.
2. Party and Customer.
3. KYC, AML and Sanctions.
4. Product Catalog, Pricing and Eligibility.
5. Account and Deposit Servicing.
6. Limits and Entitlements.
7. Ledger and Balance.
8. Payments and Transfer Orchestration.
9. Cards and Merchant Processing.
10. Lending and Collections.
11. Treasury, Liquidity and FX.
12. Fraud and Financial Crime.
13. Finance, General Ledger and Tax.
14. Reconciliation and Settlement.
15. Case, Dispute and Workflow.
16. Notification, Document and Statement.
17. Consent and Open Banking.
18. Risk, Capital and Regulatory Reporting.

### 4.4 Entegrasyon katmani

- Kafka/event streaming.
- Schema Registry ve contract governance.
- Transactional outbox CDC.
- API management ve service gateway.
- gRPC service-to-service iletisim.
- ISO 20022 message gateway.
- SWIFT, RTGS, ACH, instant payment ve kart network adaptorleri.
- Managed file transfer ve imzali batch dosyalari.
- Legacy anti-corruption layer.

Entegrasyon katmani domain mantigini yutmaz. Dis format ile canonical domain
modeli arasindaki mapping adaptorlerde yapilir ve surumlenir.

### 4.5 Veri ve zeka katmani

- Operational datastore per bounded context.
- Immutable ledger store.
- Read model/projection depolari.
- CDC ve event backbone.
- Lakehouse: raw, conformed, curated ve serving zonelari.
- Master/reference data yonetimi.
- Metadata catalog, lineage ve data quality.
- Real-time feature store ve offline feature store.
- Fraud, AML, credit ve personalization modelleri.
- Risk aggregation ve regulator reporting mart'lari.

Analitik platform operasyonel ledger'a sorgu yuklemez. Veriyi kontrollu CDC,
event veya snapshot ile alir; gecikme ve lineage her dataset icin gorunur olur.

### 4.6 Platform ve operasyon katmani

- Multi-zone Kubernetes veya esdeger orchestration.
- Service identity, service mesh veya workload mTLS.
- Secrets manager, KMS ve HSM.
- OpenTelemetry collector katmani.
- Metrics, trace, structured logs ve SIEM.
- GitOps, policy as code ve progressive delivery.
- Artifact registry, SBOM, imza ve provenance.
- Backup, PITR, immutable archive ve DR otomasyonu.
- CMDB/service catalog ve sahiplik kaydi.
- Incident, problem, change ve capacity management.

### 4.7 Network ve trust zone'lari

| Zone | Icerik | Giris/cikis kurali |
|---|---|---|
| Public edge | DNS, CDN, DDoS, WAF | Internet'ten yalniz yayinlanan TLS endpoint |
| API ingress | API gateway, BFF ingress | WAF'tan authenticated/validated trafik |
| Application | Domain servisleri | Workload identity + mTLS + network policy |
| Restricted data | Ledger, customer, KYC database | Yalniz sahip servis ve onayli operasyon yolu |
| Cardholder environment | Card switch, token vault, HSM adaptor | PCI segmenti, dar inbound/outbound |
| Integration/egress | Payment rail, SWIFT, provider adaptorleri | Allowlist, proxy/egress gateway, message security |
| Management | CI/CD, GitOps, monitoring control plane | Ayrik admin identity ve PAM |
| Security | SIEM, key management, audit archive | Append-only ingestion, cok kisitli read |
| Data/analytics | Lakehouse, warehouse, feature platform | Siniflandirilmis CDC/event; operational DB'ye ters yazma yok |

Control plane ile data plane ayrilir. Deployment veya policy yoneten control
plane'in kesilmesi mevcut finansal data plane'i otomatik durdurmamalidir. Buna
karsilik control plane credential'i ele gecirilirse blast radius namespace,
cluster, environment ve policy ile sinirlanmalidir.

## 5. Domain ve Servis Sorumluluklari

### 5.1 Identity and Access

Sahip oldugu veriler:

- Dijital kimlik, credential referansi ve federation baglantisi.
- Client/application kaydi.
- Rol, policy, authorization grant ve session.
- MFA authenticator kaydi ve recovery durumu.
- Device binding ve risk sinyali referansi.
- Token revocation/session version.

Sorumluluklari:

- Login, logout, token, step-up ve account recovery.
- Calisan, musteri, servis ve makine kimliklerini ayirmak.
- Phishing-resistant MFA; passkey/WebAuthn veya uygun PKI.
- OAuth/OIDC/FAPI politikalarini uygulamak.
- Servis kimliklerini SPIFFE benzeri workload identity ile vermek.

Tutmadigi seyler:

- Musterinin tam KYC dosyasi.
- Hesap sahipligi veya bakiye.
- Fraud kararinin authoritative kaydi.

Neden ayri: Credential guvenligi ve authentication yasam dongusu, musteri
profilinden farkli risk ve erisim sinirina sahiptir.

### 5.2 Party and Customer

Sahip oldugu veriler:

- Natural person, legal entity ve organization party kaydi.
- Adres, iletisim, tercih, dil ve vergi ikamet bilgisi.
- Party-to-party iliskileri: veli, temsilci, direktor, beneficial owner.
- Customer relationship ve segment.
- Golden customer ID ve merge/split gecmisi.

Sorumluluklari:

- Musteri profilinin tek kaynagi.
- Dedupe/entity resolution.
- Iletisim tercihleri ve consent referanslari.
- Kurumsal musteride yetkili ve sahiplik grafi.

Neden ayri: Bir party henuz banka musterisi olmayabilir; ayni party birden fazla
urun ve rolde bulunabilir. Keycloak kullanici kaydi party kaydinin yerine gecmez.

### 5.3 KYC, AML and Sanctions

Alt kabiliyetler:

- Identity proofing ve belge dogrulama.
- Liveness, yuz esleme ve sahte belge tespiti.
- PEP, sanctions ve adverse media tarama.
- Beneficial ownership ve kontrol yapisi.
- Customer risk rating.
- Enhanced due diligence.
- Periyodik review ve event-driven re-KYC.
- Transaction monitoring, alert ve suspicious activity case.
- Regulator bildirim paketi ve tipping-off korumasi.

Sahip oldugu veriler karar, evidence referansi, eslesen liste versiyonu, reviewer,
policy versiyonu ve gecerlilik suresidir. Ham belge sifreli document vault'ta
tutulur; diger servislere dagitilmaz.

Neden ayri: KYC status basit bir enum degildir. Kararin kaynagi, listelerin
versiyonu, risk faktorleri, manuel inceleme ve yeniden degerlendirme gecmisi
denetlenebilir olmalidir.

### 5.4 Product Catalog, Pricing and Eligibility

Sahip oldugu veriler:

- Urun ailesi ve surumlu urun tanimi.
- Para birimi, faiz, ucret, vergi ve kampanya kurallari.
- Eligibility, belge ve kanal kurallari.
- Effective-from/effective-to tarihleri.
- Muhasebe mapping ve ledger account template referanslari.

Urun surumu acilan hesaba snapshot/referans olarak baglanir. Sonradan urun
kuralini degistirmek eski sozlesmenin tarihsel anlamini bozmamalidir.

### 5.5 Account and Deposit Servicing

Sahip oldugu veriler:

- Hesap kimligi, IBAN veya yerel hesap numarasi.
- Party-account role ve imza/yetki modeli.
- Account lifecycle: pending, active, restricted, dormant, closed.
- Urun, sube, para birimi ve statement tercihleri.
- Bloke/hukuki kisit referanslari.

Ledger bakiyesinin sahibi degildir. Hesap servisi "bu hesap var mi ve isleme
uygun mu" sorusunu; ledger "ne kadar para var" sorusunu cevaplar.

### 5.6 Limits and Entitlements

Sahip oldugu veriler:

- Musteri, hesap, kanal, cihaz, urun ve islem tipi limitleri.
- Gunluk/aylik rolling window tuketimi.
- Kurumsal maker-checker ve imza gruplari.
- Beneficiary cooling period.
- Limit override ve onay kaniti.

Limit rezervasyonu ve kesinlestirme ayri adimlardir. Basarisiz odemede rezervasyon
serbest birakilir. Counter update atomik ve idempotent olmalidir.

### 5.7 Ledger and Balance

Bankanin parasal gerceginin kaynagidir. Journal, posting, ledger account, hold,
balance snapshot ve finansal idempotency kaydini yonetir. Ayrintisi Bolum 8'dedir.

### 5.8 Payments and Transfer Orchestration

Sahip oldugu veriler:

- Payment instruction ve end-to-end kimlik.
- Payer/payee, route, rail ve fee secimi.
- Payment state machine.
- Her dis gonderim denemesi ve acknowledgement.
- Cancellation, return, recall ve investigation baglantilari.

Ledger posting'in sahibi degildir. Odemenin is surecini yonetir; ledger'a typed
komut gonderir. External rail adaptorleri ayri anti-corruption layer'dir.

### 5.9 Cards and Merchant Processing

Alt kabiliyetler:

- Card lifecycle, token provisioning ve PIN/HSM entegrasyonu.
- Authorization switch ve stand-in politikasi.
- Merchant/MCC, velocity ve card controls.
- Authorization hold.
- Clearing, presentment, settlement ve interchange.
- Chargeback, representment ve dispute.
- 3-D Secure ve card-not-present risk akisi.

PAN tokenlestirilir; CVV/PIN uygulama loguna veya genel veritabanina girmez.
Authorization ile clearing ayni kayit degildir; kismi capture, incremental auth,
reversal ve offline advice desteklenir.

### 5.10 Lending and Collections

Alt kabiliyetler:

- Application/origination.
- Affordability, bureau ve credit decision.
- Offer, pricing, collateral ve agreement.
- Disbursement.
- Amortization schedule ve accrual.
- Repayment allocation.
- Delinquency, forbearance, restructuring ve collections.
- Provisioning/ECL veri uretimi.

Kredi schedule'i sozlesme gercegidir; para hareketleri ledger'da posting'dir.
Credit decision modeli ve feature snapshot'i karar kaniti olarak versiyonlanir.

### 5.11 Treasury, Liquidity and FX

- FX rate source, quote, spread ve quote expiry.
- Nostro/vostro pozisyonlari.
- Intraday liquidity ve funding.
- Cash flow forecast.
- ALM ve IRRBB girdileri.
- Money market, securities ve hedge islemleri.
- Counterparty ve market limitleri.

Musteri bakiyesi ile bankanin settlement likiditesi ayni sey degildir. Musteri
odemesi kabul edilmeden once ilgili rail/nostro likiditesi ve cut-off kurali
gerekebilir.

### 5.12 Fraud and Financial Crime

- Device, session, beneficiary, behavioral ve transaction sinyalleri.
- Gercek zamanli feature hesaplama.
- Rule engine ve model ensemble.
- Allow, deny, challenge, review karari.
- Model/rule version, reason code ve explainability.
- Feedback, confirmed fraud ve model monitoring.

Fraud servisi kaynak transaction'i degistirmez. Imzali/versiyonlu karar verir;
odeme servisi karari state machine'e uygular. Timeout fallback'i islem turune ve
risk tutarina gore fail-closed veya kontrollu fail-open olabilir.

### 5.13 Finance, General Ledger and Tax

- Chart of accounts.
- Subledger-to-GL mapping.
- Accounting period ve close.
- Trial balance, P&L ve balance sheet.
- Accrual, amortization, depreciation ve tax posting.
- Intercompany ve consolidation.
- Suspense account governance.

Operasyonel ledger ile kurumsal GL ayrilabilir; her operational posting,
deterministik accounting event ile GL'a izlenebilir. GL gecikmeli olabilir fakat
toplamlar reconciliation ile eslesmelidir.

### 5.14 Reconciliation and Settlement

- Internal ledger-to-ledger reconciliation.
- Rail/network acknowledgement mutabakati.
- Clearing file ve settlement account mutabakati.
- Nostro statement mutabakati.
- Card scheme ve merchant settlement mutabakati.
- Break, aging, materiality ve case yonetimi.

Bu fonksiyon rapor degildir; para kaybi ve eksik/tekrarli islemi bulan kontrol
katmanidir. Otomatik esleme, tolerance, exception queue ve insan onayi gerekir.

### 5.15 Case, Dispute and Workflow

Uzun sureli, insan adimli surecler icin kullanilir:

- KYC manual review.
- AML/fraud investigation.
- Payment investigation ve recall.
- Card dispute/chargeback.
- Loan exception ve collections.
- Customer complaint.
- Operational repair.

Workflow motoru ledger degildir. Surec durumunu ve gorevleri tutar; parasal
degisiklikler ilgili domain komutuyla yapilir.

### 5.16 Notification, Document and Statement

- Kanal tercihi ve template versiyonu.
- E-posta, SMS, push ve guvenli inbox teslimi.
- OTP ile pazarlama bildirimi birbirinden ayrilir.
- Document generation, e-signature ve immutable archive.
- Periodic statement ve on-demand dekont.
- Delivery receipt ve tekrar politikasi.

Bildirim hatasi finansal transaction'i rollback etmez. Event'ten tekrar
uretilebilir ve delivery state ayri izlenir.

### 5.17 Consent and Open Banking

- Consent scope, purpose, account listesi ve expiry.
- TPP kaydi, sertifika ve software statement.
- Authorization journey ve SCA.
- Consent revoke ve audit.
- Account information ve payment initiation.
- Dynamic client ve API quota yonetimi.

Token scope tek basina yeterli degildir; resource service consent'in aktifligini,
hesap kapsamlarini ve islem yetkisini dogrular.

### 5.18 Risk, Capital and Regulatory Reporting

- Credit, market, liquidity ve operational risk aggregation.
- Exposure, collateral ve counterparty gorunumu.
- Stress test ve scenario.
- Capital/liquidity metrikleri.
- Regulatory returns ve evidence.
- IFRS 9/ECL veri setleri.
- BCBS 239 lineage, completeness, timeliness ve reconciliation.

Bu katman operasyonel servislerden manuel Excel kopyalariyla beslenmez. Kontrollu
lineage, veri kalitesi, onay ve yeniden uretilebilir snapshot gerekir.

## 6. Veri Sahipligi ve Depolama Modeli

### 6.1 Depo secim kurallari

| Ihtiyac | Uygun depo | Kullanilmamasi gereken rol |
|---|---|---|
| Transactional domain state | PostgreSQL/kurumsal RDBMS | Servisler arasi ortak database |
| Ledger journal/posting | ACID RDBMS veya kanitlanmis ledger store | Redis veya eventual search index |
| Event backbone | Kafka | Tek finansal system of record |
| Cache/rate limit | Redis | Authoritative bakiye veya KYC karari |
| Arama | OpenSearch/Elasticsearch | Musteri/hesap authoritative kaydi |
| Belge ve archive | Sifreli object storage, gerekirse WORM | Canli transaction lock mekanizmasi |
| Analitik | Lakehouse/warehouse | Gercek zamanli posting motoru |
| Iliski/ownership analizi | Graph projection | Party ana kaydinin tek basina yerine gecmek |

### 6.2 Veri siniflandirmasi

- Public: yayinlanabilir urun bilgisi.
- Internal: operasyonel fakat musteri sirri olmayan veri.
- Confidential: musteri, hesap, islem ve risk verisi.
- Restricted: credential, kimlik belgesi, PAN, biyometri, AML case, anahtar
  materyali.

Her alan icin owner, steward, hukuki dayanak, saklama suresi, maskeleme,
tokenization, erisim rolleri ve imha/anonimlestirme politikasi bulunur.

### 6.3 Zaman modeli

- Tum teknik timestamp'ler UTC ve timezone-aware tutulur.
- Business date, value date, booking date ve settlement date ayridir.
- Sure olcumunde monotonic clock kullanilir.
- Is gunu, tatil, cut-off ve timezone kurallari merkezi ama versiyonlu Business
  Calendar servisinden gelir.
- Uygulama kodu dogrudan makine saatine baglanmaz; test edilebilir Clock kullanir.

### 6.4 Kimlikler

- Dahili primary key tahmin edilemeyen UUID/ULID olabilir.
- Harici payment ID, end-to-end ID ve scheme reference ayridir.
- Idempotency key istemci + operation + scope ile benzersizdir.
- Correlation ID trace'i; causation ID hangi event/komutun sonucu oldugunu
  belirtir.
- PII veya hesap numarasi Kafka partition key/log etiketi olarak acik kullanilmaz;
  token veya kontrollu surrogate kullanilir.

## 7. API, gRPC ve Event Sozlesmeleri

### 7.1 HTTP API standardi

Her endpoint sunlari tanimlar:

- OpenAPI sozlesmesi ve ornekleri.
- Authentication, authorization ve consent gereksinimi.
- Idempotency davranisi.
- Request/response boyut ve sayfalama limiti.
- Problem Details uyumlu hata kodu.
- Optimistic concurrency icin version/ETag.
- Timeout ve retry guvenligi.
- PII siniflandirmasi.
- Deprecation ve sunset tarihi.

POST tekrarlarinda ayni idempotency key ve ayni payload ayni business sonucu
dondurur. Ayni anahtarla farkli payload conflict verir. Idempotency kaydi devam
eden, basarili ve basarisiz terminal durumlari ayirt eder.

### 7.2 gRPC standardi

- Proto3 ve package/version standardi.
- Buf veya esdeger breaking-change kontrolu.
- Deadline zorunlulugu ve deadline propagation.
- Retry yalniz idempotent metotlarda.
- mTLS workload identity.
- Status code + typed error detail.
- Field mask ile kontrollu update.
- Reflection production'da kontrollu.

### 7.3 Event envelope

Her event en az su semantigi tasir:

- eventId ve eventType.
- schemaVersion.
- aggregateType ve aggregateId.
- aggregateVersion/sequence.
- occurredAt ve publishedAt.
- producer ve tenant/legal entity.
- correlationId ve causationId.
- idempotency/business key.
- dataClassification.
- trace context.
- payload ve gerekirse subject reference.

Event adi gecmis zamanli gercegi anlatir: `PaymentAccepted`, `HoldPlaced`,
`PostingCommitted`. `ProcessPayment` event degil komuttur.

### 7.4 Event evolution

- Schema Registry compatibility gate kullanilir.
- Alan ekleme backward-compatible yapilir.
- Anlam degisikligi yeni alan veya major event gerektirir.
- Consumer bilinmeyen alani tolere eder.
- Producer ve consumer sahipleri katalogda gorunur.
- Retention, replay, compaction ve privacy politikasi topic bazinda tanimlidir.
- Poison message retry topic ve DLT'ye gider; DLT mezarlik degil sahipli kuyruktur.

### 7.5 Senkron ve asenkron secim

Senkron cagri kullan:

- Istemci sonucu beklemeden devam edemiyorsa.
- Anlik yetki veya bakiye karari gerekiyorsa.
- Cevap kisa surede ve belirli timeout icinde uretiliyorsa.

Event kullan:

- Gerceklesmis durumu birden fazla tuketiciye duyurmak icin.
- Bildirim, projection, analitik ve downstream muhasebe gibi yan etkiler icin.
- Replay ve bagimsiz olcekleme gerekiyorsa.

Uzun sureli workflow kullan:

- Insan gorevi, saatler/gunler suren dis cevap veya timer varsa.
- State machine ve kompanzasyon gorunur olmaliysa.

Batch/file kullan:

- Scheme veya regulator bunu gerektiriyorsa.
- Buyuk hacimli cut-off tabanli clearing/statement varsa.
- Dosya checksum, imza, sequence, control total ve acknowledgement ile yonetilir.

### 7.6 Kaynaktan hedefe iletisim matrisi

| Kaynak | Hedef | Tasima/sozlesme | Tasidigi veri | Neden |
|---|---|---|---|---|
| Mobile/Web | Edge | TLS HTTPS | OIDC/FAPI istekleri, kanal DTO | Internet siniri, standard client guvenligi |
| Edge | BFF/API | TLS HTTP | Validated request, token, trace context | Kanal birlestirme ve policy enforcement |
| BFF/API | Domain command service | mTLS gRPC veya HTTP | Typed command, idempotency key, deadline | Anlik business sonucu gerekli |
| Domain service | Kendi DB'si | TLS DB protocol | Authoritative domain state | Yerel ACID transaction |
| Domain service | Kendi outbox tablosu | Ayni DB transaction | Domain event envelope/payload | Dual-write kaybini engellemek |
| Outbox CDC | Kafka | Kafka TLS/SASL | Committed domain event | Guvenilir asenkron dagitim |
| Kafka | Projection consumer | Kafka consumer group | Sirali aggregate eventleri | Bagimsiz okuma modeli ve replay |
| Payment orchestration | Ledger | mTLS gRPC/typed command | Hold, post, release, reverse | Parasal karar anlik ve authoritative |
| Payment orchestration | Fraud/Limit | mTLS gRPC | Risk context ve reservation | Kritik yolda karar gerekir |
| Payment rail adaptor | External rail | ISO 20022, ISO 8583, SWIFT veya scheme API/file | Scheme mesaji | Dis standardi domain'den izole etmek |
| External rail | Rail adaptor | Ayni scheme protokolu | Ack, status, clearing, return | Transport ve business durumu ayirmak |
| Subledger | Finance/GL | Kafka accounting event veya kontrollu batch | Posting ozeti ve mapping anahtari | Operational posting'i kurumsal muhasebeye tasimak |
| Operational servisler | Lakehouse | CDC/Kafka/snapshot | Siniflandirilmis degisim verisi | Analitigi OLTP'den ayirmak |
| Servisler | OpenTelemetry Collector | OTLP mTLS | Trace, metric, filtreli log | Vendor-neutral korelasyon |
| Servisler | Audit/Evidence | Append-only API/event | Actor, karar, policy ve evidence referansi | Denetim ve non-repudiation |
| Notification | Provider | TLS provider API/SMPP/e-posta transportu | Minimum mesaj ve delivery ID | Finansal akistan ayrik teslim |
| CI/CD | Artifact registry | TLS + signed OCI | Image, SBOM, provenance | Immutable ve dogrulanabilir release |
| GitOps | Runtime control plane | Kubernetes API/mTLS | Deklaratif desired state | Auditli deployment ve drift kontrolu |

Header ile tasinmasi gereken teknik context minimumdur: trace context,
correlation ID, idempotency key, authenticated principal reference ve gerekli
locale/channel bilgisi. Tam token, musteri profili veya yetki karari event/event
header'larina kopyalanmaz.

## 8. Ledger ve Bakiye Mimarisi

### 8.1 Ana veri modeli

- Ledger: yasal varlik ve muhasebe kapsami.
- LedgerAccount: varlik, yukumluluk, gelir, gider veya equity hesabi.
- CustomerAccount: urun/servicing gorunumu; bir veya daha fazla ledger hesaba
  baglanabilir.
- JournalEntry: tek ekonomik olayin basligi.
- Posting: bir ledger hesaba debit veya credit satiri.
- PostingBatch: atomik kaydedilen posting grubu.
- Hold/Reservation: kullanilabilir bakiyeyi azaltan gecici rezervasyon.
- BalanceSnapshot: posting'lerden turetilmis hizli okuma gorunumu.
- AccountingEvent: operational ledger'dan GL mapping girdisi.
- ReversalLink: duzeltme kaydini orijinal kayda baglar.

### 8.2 Bakiye turleri

- Ledger/booked balance: deftere kaydedilmis hareketlerin toplami.
- Available balance: booked balance, hold, overdraft ve uygun pending hareketler
  dikkate alinarak harcanabilir tutar.
- Pending balance: henuz booked/final olmayan hareketler.
- Hold/reserved amount: authorization veya bekleyen odeme icin ayrilan tutar.
- Value-dated balance: faiz ve valute hesaplamasinda kullanilan bakiye.
- Cleared balance: clearing kosullarini tamamlamis tutar.

Tek bir `money` ve `blockedMoney` alani bu semantigi tasiyamaz.

### 8.3 Posting kurallari

- Her batch dengeli olmalidir.
- Posting satirlari immutable'dir.
- Amount decimal ve currency ile birlikte zorunludur; float/double kullanilmaz.
- Cross-currency journal'da her currency kendi icinde dengelenir; FX bridge ve
  gain/loss hesaplari kullanilir.
- Negatif bakiye urun/overdraft kuralina tabidir.
- Account status, legal hold ve debit/credit permission kontrol edilir.
- Journal idempotency key unique constraint ile korunur.
- Aggregate/account version ile concurrent update engellenir.
- Snapshot bozulursa posting logundan yeniden hesaplanabilir.

### 8.4 Hold yasam dongusu

1. Talep ve tutar dogrulanir.
2. Available balance atomik kontrol edilir.
3. Hold benzersiz business key ile olusturulur.
4. Available balance projection'i ayni transaction'da guncellenir.
5. `HoldPlaced` outbox'a yazilir.
6. Capture gelirse hold kismen veya tamamen posting'e doner.
7. Reversal/expiry gelirse kullanilmayan kisim serbest kalir.
8. Expiry worker deterministik ve idempotent calisir.

### 8.5 Reversal ve duzeltme

Kayit silinmez. Orijinal posting'in ters yonlu ayni tutarli kaydi, reason code,
aktor ve orijinal journal baglantisiyla yazilir. Sonra gerekiyorsa dogru yeni
journal yazilir. Settlement sonrasi return, reversal ile ayni business olay
degildir; farkli payment state ve scheme referansi gerekir.

### 8.6 Ledger olcekleme

- Partition/shard anahtari yasal varlik + currency + account grubu olabilir.
- Tek account'a ait posting sirasini tek authoritative writer/partition korur.
- Ayni shard transfer yerel atomik batch olabilir.
- Cross-shard transfer icin kanitlanmis coordinator, reservation ve idempotent
  finalize gerekir; iki bagimsiz bakiyeyi rastgele guncellemek kabul edilmez.
- Sicak hesaplar icin partition hot-spot analizi yapilir.
- Read projection'lar ayrica olceklenir; write ledger'a rapor sorgusu gitmez.
- Active-active write ancak partition ownership/fencing ve split-brain kaniti
  varsa kullanilir. Finansal kayitta last-write-wins uygulanmaz.

## 9. Dagitik Transaction ve Mesajlasma Modeli

### 9.1 Transactional outbox

Domain state ve outbox mesaji ayni lokal database transaction'inda yazilir. CDC
connector committed outbox satirini Kafka'ya tasir. Publisher sonucunu beklemeden
DB commit edip event'i kaybetme riski boyle kaldirilir.

### 9.2 Idempotent inbox

Consumer, event ID/business key kaydini ve domain degisikligini ayni lokal
transaction'da yapar. Ayni mesaj tekrar geldiginde sonuc tekrar uretilmez.
Offset ancak domain commit'inden sonra ilerler veya Kafka transaction modeliyle
uygun sekilde koordine edilir.

### 9.3 Saga

Saga su durumlarda uygundur:

- Birden fazla bounded context uzun sureli is surecine katiliyorsa.
- Her adimin idempotent komutu ve acik kompanzasyonu varsa.
- Intermediate state business tarafindan kabul edilebiliyorsa.

Saga su durumda uygun degildir:

- Double-entry posting'in iki satirini ayri servislerde eventual consistent
  yazmak icin.
- Kayip parayi sonradan "belki" duzeltmek icin.
- Belirsiz state machine'i try/catch zinciriyle gizlemek icin.

### 9.4 Retry, timeout ve circuit breaker

- Her remote call'in deadline'i vardir.
- Retry budget ve exponential backoff + jitter kullanilir.
- Non-idempotent komut otomatik retry edilmez; once status/idempotency sonucu
  sorgulanir.
- Circuit breaker hata izolasyonu saglar fakat business fallback tanimli olmadan
  veri dogrulugunu cozmez.
- Bulkhead kritik pool'lari raporlama ve bildirim yukunden ayirir.
- Backpressure ve admission control, asiri yukte kontrollu reddetme saglar.

### 9.5 Standart payment state modeli

| Durum | Anlam | Izinli sonraki ornek durumlar |
|---|---|---|
| RECEIVED | Istek durable olarak alindi | VALIDATING, REJECTED |
| VALIDATING | Hesap, entitlement, compliance kontrolleri | AUTHORIZATION_REQUIRED, FUNDS_RESERVING, REJECTED |
| AUTHORIZATION_REQUIRED | SCA/maker-checker bekleniyor | VALIDATING, EXPIRED, CANCELLED |
| FUNDS_RESERVING | Limit ve para rezervasyonu isleniyor | FUNDS_RESERVED, REJECTED, UNKNOWN |
| FUNDS_RESERVED | Hold/limit basarili | SENDING, CANCELLED |
| SENDING | Dis raya gonderim deneniyor | ACCEPTED_EXTERNAL, REJECTED, UNKNOWN |
| ACCEPTED_EXTERNAL | Rail islemi kabul etti | PROCESSING, COMPLETED, RETURNED |
| PROCESSING | Clearing/settlement bekleniyor | COMPLETED, RETURNED, INVESTIGATION |
| UNKNOWN | Sonuc kesin bilinmiyor | PROCESSING, COMPLETED, REJECTED, INVESTIGATION |
| COMPLETED | Musteri ve rail semantigine gore tamamlandi | RETURNED, INVESTIGATION |
| REJECTED | Terminal kabul edilmeme | Yeni instruction disinda gecis yok |
| CANCELLED | Gonderimden once iptal | Terminal |
| RETURNED | Daha once kabul/tamamlanan tutar geri dondu | Terminal veya investigation |
| INVESTIGATION | Manuel/scheme arastirmasi | COMPLETED, RETURNED, REJECTED |

State gecisi compare-and-set/version ile yapilir. Her gecis actor, reason,
external reference ve time ile history'ye yazilir. `UNKNOWN` hata mesaji degil,
bilincli ve izlenen bir business durumudur; reconciliation/status inquiry
tamamlanmadan tekrar gonderim yapilmaz.

## 10. Uctan Uca Veri Akislari

Bu bolumdeki akislar logical referanstir. Servis adlari deployment sayisini
zorunlu kilmaz; ayni bounded context moduler monolith veya birden fazla deployment
olabilir. Kritik olan veri sahipligi ve transaction siniridir.

### 10.1 Retail musteri onboarding ve ilk hesap acilisi

Katilanlar:

- Mobile/Web BFF.
- Identity and Access.
- Party/Customer.
- Document Vault.
- Identity Proofing.
- KYC/AML/Sanctions.
- Product/Eligibility.
- Account.
- Ledger.
- Workflow/Case.
- Notification.
- Audit/Evidence.

Basarili akis:

1. Kanal cihaz attestasyonu, bot kontrolu, IP/device risk ve rate limit uygular.
2. Kullanici telefon/e-posta sahipligini OTP veya uygun kanal ile kanitlar.
3. Identity servisi henuz tam aktif olmayan enrollment kimligi olusturur.
4. Party servisi idempotency key ile aday party kaydi acar; dedupe sonucu
   possible-match ise otomatik birlestirme yapmaz, review acar.
5. Kimlik belgesi dogrudan sifreli Document Vault'a yuklenir; servisler signed
   reference kullanir, belgeyi event payload'ina koymaz.
6. Proofing servisi belge authenticity, OCR, liveness ve face match sonuclarini
   kanit referanslariyla KYC'ye yollar.
7. KYC; sanctions, PEP, adverse media, ulke, meslek, gelir ve urun riskini policy
   versiyonuyla degerlendirir.
8. Sonuc `APPROVED`, `REJECTED` veya `MANUAL_REVIEW` olur. Basit boolean yeterli
   degildir; reason, evidence ve valid-until tutulur.
9. Product/Eligibility secilen urunun bu musteri, kanal ve yargi alanina uygun
   oldugunu kontrol eder.
10. Customer relationship aktiflestirilir.
11. Account servisi product version'a bagli hesap acar, benzersiz IBAN/numara
    tahsis eder ve party-account role kaydini yazar.
12. Ledger servisi hesabin gerekli subledger hesaplarini idempotent olarak acar.
13. Account ancak ledger account referanslari basariyla donunce `ACTIVE` olur.
14. Identity enrollment aktif edilir ve gerekli MFA kaydi tamamlanir.
15. `CustomerOnboarded` ve `AccountOpened` eventleri outbox ile yayinlanir.
16. Notification hos geldin/sozlesme bildirimini gonderir; basarisiz bildirim
    onboarding'i geri almaz.
17. Tum karar zinciri audit/evidence store'a yazilir.

Hata ve toparlama:

- Proofing/KYC reddinde identity enrollment kapatilir, retention gerektiren KYC
  kanitlari hukuki sure boyunca saklanir.
- Manual review durumunda workflow bekler; tekrar ayni party ve hesap acilmaz.
- Account acildi, ledger acilamadiysa hesap `PROVISIONING_FAILED` kalir; retry ayni
  provisioning key ile yapilir. Kayit silinip yeni ID uretilmez.
- Identity aktivasyonu basarisizsa finansal hesap erisime acilmaz; operasyonel
  repair queue olusur.
- Timeout alan kanal status endpoint'inden onboarding ID ile sonucu sorgular.

Neden bu sira: Kimligi kanitlanmamis veya sanctions karari tamamlanmamis kisiye
aktif finansal hesap acilmaz; buna karsilik uzun KYC sureci boyunca credential ve
party adayinin kontrollu intermediate state'te tutulmasi gerekir.

### 10.2 Kurumsal musteri onboarding

Retail akisa ek olarak:

1. Legal entity registry bilgisi ve vergi kaydi alinir.
2. Director, authorized signatory ve beneficial owner party'leri ayri kaydedilir.
3. Ownership/control grafi kurulur ve nihai gercek kisi sahipler belirlenir.
4. Her ilgili kisi KYC/PEP/sanctions kontrolunden gecer.
5. Kurumun faaliyet, ciro, ulke, sektor ve beklenen islem profili kaydedilir.
6. Maker-checker, imza grubu, tutar esigi ve kanal yetkileri Entitlement servisinde
   tanimlanir.
7. Ticari urun, nakit yonetimi, toplu odeme ve limitler ayrica onaylanir.
8. Sozlesmeler e-imza ve document archive ile baglanir.

Degisiklik akisi de onboarding kadar kritiktir. Beneficial owner, direktor veya
imza yetkisi degisince event-driven re-KYC ve entitlement review tetiklenir.

### 10.3 Login, cihaz baglama ve step-up authentication

1. Kanal authorization request'i baslatir; PKCE/PAR ve kesin redirect URI
   kontrolu uygulanir.
2. Identity kullanici, authenticator ve session riskini kontrol eder.
3. Device intelligence yeni cihaz, root/jailbreak, emulator, malware ve impossible
   travel sinyallerini uretir.
4. Dusuk riskte uygun AAL ile login; yuksek riskte phishing-resistant step-up
   istenir.
5. Authorization server kisa omurlu, audience/scope kisitli ve sender-constrained
   token verir.
6. Gateway signature, issuer, audience, expiry ve sender binding dogrular.
7. Resource service object-level authorization, consent ve account ownership'i
   yeniden kontrol eder.
8. Session ve risk eventleri fraud platformuna gider.

Token URL query parameter'inda tasinmaz. Logout yalniz istemci cookie'sini silmek
degildir; session revoke ve kritik cihaz/credential olayinda token/session version
gecersizlestirme gerekir.

### 10.4 Hesap acma ve urun degisikligi

1. Kanal product catalog'dan uygun, effective urunleri okur.
2. Eligibility musteri segmenti, KYC, yas, ulke ve gerekli belgeyi kontrol eder.
3. Pricing musteriye ozel faiz/ucret teklifini ve expiry'yi verir.
4. Musteri teklif snapshot'ini onaylar.
5. Account sozlesme ve product version referansiyla acilir.
6. Ledger hesaplari ve accounting mapping olusturulur.
7. Initial funding gerekiyorsa ayri payment instruction baslatilir.
8. Account eventleri statement, CRM ve data platform projection'larini besler.

Urun degisikligi eski kaydi sessizce mutate etmez. Effective-dated amendment,
customer notice ve gerekiyorsa yeni accounting mapping kullanilir.

### 10.5 Nakit yatirma

Kanal sube veya ATM olabilir.

1. Terminal/vezne kimligi, cihaz durumu ve operator yetkisi dogrulanir.
2. Hesap durumu, currency, AML threshold ve cash acceptance kurali kontrol edilir.
3. ATM deposit'te sayim sonucu provisional olabilir; zarfli yatirma hemen final
   bakiye yaratmayabilir.
4. Cash transaction benzersiz terminal sequence ve idempotency key ile olusur.
5. Ledger, cash-in-transit/branch cash hesabi ile customer liability hesabi
   arasinda dengeli posting yapar.
6. Supheli/yuksek tutar event'i AML monitoring'e gider.
7. ATM fiziksel sayim ile electronic journal reconciliation'a girer.
8. Fark varsa customer posting silinmez; adjustment case ve yetkili posting
   kullanilir.

### 10.6 ATM nakit cekme

1. Kart/chip ve PIN HSM uzerinden dogrulanir; PIN uygulamaya acik gelmez.
2. Card/account status, ATM ve ulke riski kontrol edilir.
3. Limits gunluk ATM tuketimini atomik rezerve eder.
4. Fraud gercek zamanli karar verir.
5. Ledger available balance uzerinde hold olusturur.
6. ATM dispense komutu alir ve fiziksel sonuc advice mesaji gonderir.
7. Dispense basariliysa hold posting'e capture edilir.
8. Dispense basarisizsa hold release edilir.
9. Cevap kaybolursa islem `UNKNOWN` kabul edilir; terminal electronic journal,
   cash cassette sayimi ve switch logu ile reconciliation yapilir.
10. Para verilmedigi kanitlanmadan otomatik ikinci debit yapilmaz.

### 10.7 Banka ici book transfer

1. Kanal idempotency key ve beneficiary bilgisiyle payment command gonderir.
2. Payment servisi source/destination hesap ve party yetkisini dogrular.
3. Limits ve fraud karari alinir.
4. Iki hesap ayni ledger transaction sinirindaysa debit ve credit posting'leri tek
   dengeli journal'da atomik yazilir.
5. Fee varsa gelir hesabi ayni journal'a eklenir.
6. Payment state `COMPLETED` olur ve outbox event'i yayinlanir.
7. Notification, statement ve analytics event'i tuketir.

Kanal timeout alsa bile ayni idempotency key ile sonuc sorgular. Debit basarili,
credit belirsiz gibi bir ara durum yerel book transferde kabul edilmez.

### 10.8 Bankalar arasi anlik odeme - giden

1. Payment instruction kabul edilir, semantik ve beneficiary format kontrolu
   yapilir.
2. Account/entitlement, KYC restriction, sanctions, fraud ve limits kontrol edilir.
3. Gerekirse Confirmation of Payee/name check uygulanir.
4. Ledger source hesapta tutar + fee hold'u koyar.
5. Payment state `FUNDS_RESERVED` olur.
6. Rail adaptor canonical modeli ilgili ISO 20022/yerel mesaja cevirir.
7. Mesaj benzersiz scheme reference, sequence ve cryptographic transport ile
   gonderilir.
8. Rail teknik/business acknowledgement verir.
9. Kesin kabul/finality modeline gore hold capture edilerek customer debit ve
   settlement/nostro clearing hesabi credit edilir.
10. Reject gelirse hold release edilir ve payment `REJECTED` olur.
11. Timeout/cevap kaybinda payment `UNKNOWN/PENDING_EXTERNAL` kalir; otomatik yeni
    message ID ile tekrar gonderilmez. Status inquiry veya reconciliation sonucu
    beklenir.
12. Settlement ve rail statement daha sonra reconciliation'da posting'le eslesir.

Musteriye gosterilen `accepted`, `processing`, `completed`, `settled`, `returned`
durumlari ayni degildir. UI bu semantigi kaybetmemelidir.

### 10.9 Bankalar arasi anlik odeme - gelen

1. Rail gateway mesaja transport authentication, signature, duplicate ve schema
   kontrolu uygular.
2. Scheme message ID inbox'ta tekillestirilir.
3. Beneficiary account ve routing kontrol edilir.
4. Sanctions/AML politikasi uygulanir; mevzuata gore hold/review gerekebilir.
5. Ledger settlement/clearing hesabi debit, customer liability hesabi credit
   posting'i yapar.
6. Rail'e kabul cevabi yalniz durable posting tamamlandiktan sonra verilir.
7. Payment record inbound reference ile tamamlanir.
8. Notification ve transaction history event'ten guncellenir.
9. Sonraki settlement statement ile mutabakat yapilir.

### 10.10 ACH/EFT ve planli odeme

1. Standing order veya schedule business calendar ile due hale gelir.
2. Scheduler yalniz tetikler; payment servisi ayni instruction icin unique run ID
   ile komutu sahiplenir.
3. Cut-off, tatil, para birimi ve next-business-day kurali uygulanir.
4. Funds reservation veya debit, yerel rail kuralina gore yapilir.
5. Outbound clearing batch control total, adet, hash ve imzayla uretilir.
6. Managed file transfer veya scheme channel uzerinden gonderilir.
7. File-level ve item-level acknowledgement ayri islenir.
8. Reject/return kaydi orijinal item'a baglanir.
9. Net settlement posting'i ile item toplamlarinin kontrol hesaplari mutabik olur.

### 10.11 Toplu maas/kurumsal odeme

1. Kurumsal musteri dosya/API batch'i yukler.
2. Dosya imza, schema, duplicate, control total ve yetki kontrolunden gecer.
3. Her kalem validate edilir; batch tumden veya kismi kabul politikasi aciktir.
4. Maker islemi olusturur, checker imza matrisi ve tutar esigine gore onaylar.
5. Funding kontrol edilir ve batch rezervasyonu yapilir.
6. Kalemler partition edilerek odeme motoruna dagitilir.
7. Her item ayri idempotency ve durum tasir; batch aggregate sonucu turetilir.
8. Kurumsal kanala pain.002 benzeri status/rapor verilir.
9. Basarisiz kalemler tekrar yuklemeyle cift odeme yaratmaz.

### 10.12 Cross-border/SWIFT odeme

1. Originator, beneficiary, intermediary, charge bearer ve purpose bilgisi
   canonical payment modeline alinir.
2. FATF wire transfer veri zorunluluklari kontrol edilir.
3. Sanctions screening ad, banka, ulke, BIC, serbest metin ve ilgili taraflarda
   yapilir.
4. FX gerekiyorsa expiry'li quote kilitlenir.
5. Fees ve correspondent route hesaplanir.
6. Customer funds hold/debit edilir; payment investigation'a uygun referanslar
   uretilir.
7. ISO 20022 CBPR+/ilgili network mesaji mapping ve validation'dan gecer.
8. SWIFT/network acknowledgement, correspondent status ve account statement
   ayri kaydedilir.
9. Nostro posting ve customer posting linkli fakat farkli hesap katmanlaridir.
10. Return, reject, cancellation request ve recall kendi state transition'laridir.
11. FX farki, correspondent fee ve rounding ayri ledger hesaplarina post edilir.
12. Nostro reconciliation gercek settlement'i dogrular.

### 10.13 Direct debit mandate ve tahsilat

1. Mandate kim tarafindan, hangi hesap, creditor, limit ve sure icin verildigiyle
   kaydedilir.
2. E-signature/SCA evidence saklanir.
3. Collection request aktif mandate ve notice kuralina gore dogrulanir.
4. Duplicate collection ID reddedilir.
5. Funds ve limit kontrolu yapilir; rail kuralina gore debit/hold uygulanir.
6. Clearing ve settlement tamamlanir.
7. Musterinin refund/itiraz hakki icin scheme deadline saklanir.
8. Mandate revoke gelecekteki tahsilatlari engeller, gecmisi silmez.

### 10.14 Kart authorization - POS veya e-commerce

1. Network/switch ISO 8583 veya scheme mesajini alir ve duplicate kontrol eder.
2. Token/PAN vault karti, hesabi ve customer'i cozer; hassas veri servisler arasi
   yayilmaz.
3. Card status, expiry, channel, MCC, country, contactless/e-commerce control ve
   velocity limit kontrol edilir.
4. 3-D Secure sonucu ve cryptogram gibi kanitlar degerlendirilir.
5. Fraud modeli milisaniye butcesinde karar verir.
6. FX/DCC ve available balance kurallari uygulanir.
7. Ledger authorization hold'u atomik yerlestirir.
8. Switch approve/decline reason code ile cevap verir.
9. Advice/reversal gelirse hold guncellenir veya kaldirilir.
10. Network timeout'ta stand-in processing yalniz onayli risk ve floor limit
    politikasi ile yapilir.

Authorization para transferinin final hali degildir. Merchant daha sonra farkli
tutarla kismi/tam clearing sunabilir; tip, otel veya akaryakit gibi incremental
authorization senaryolari desteklenir.

### 10.15 Kart clearing ve settlement

1. Scheme clearing dosyasi/stream'i sequence, hash, imza ve duplicate kontroluyle
   alinir.
2. Presentment orijinal authorization ile eslestirilir.
3. Authorized amount, clearing amount, currency, FX, interchange ve fee ayrilir.
4. Hold capture edilir; fazla kisim release edilir veya kurala gore ek kontrol
   acilir.
5. Customer, scheme receivable/payable, interchange ve fee hesaplarina dengeli
   posting yazilir.
6. Unmatched presentment exception queue'ya gider; sessizce kaybolmaz.
7. Scheme settlement reportu, settlement bank hesabi ve ledger mutabik edilir.
8. Statement'ta authorization ve booked transaction semantigi dogru gosterilir.

### 10.16 Kart dispute ve chargeback

1. Musteri booked card transaction icin dispute acar.
2. Eligibility scheme reason, sure ve onceki refund durumuna gore kontrol edilir.
3. Case evidence, belge ve iletisimleri yonetir.
4. Gerekiyorsa provisional credit ayri ledger posting'i olarak verilir.
5. Chargeback scheme'e gonderilir; acknowledgement ve deadline izlenir.
6. Merchant representment evidence'i incelenir.
7. Kazanma/kaybetme sonucunda provisional ve final hesaplar kapatilir.
8. Fraud feedback model egitimine kontrollu etiket olarak gider.

### 10.17 Kredi basvurusu ve kullandirim

1. Application servisi basvuru ve consent'i kaydeder.
2. Party/KYC, income, bureau, affordability, exposure ve fraud verileri alinir.
3. Feature snapshot ve veri zamani dondurulur.
4. Credit decision rule/model versiyonu, score, reason ve override ile kaydedilir.
5. Uygunsa pricing risk bazli teklif ve expiry uretir.
6. Musteri sozlesmeyi onaylar; collateral/guarantee gerekiyorsa perfection
   tamamlanir.
7. Loan account ve amortization schedule acilir.
8. Ledger principal receivable ve disbursement posting'ini dengeli yazar.
9. Para beneficiary/customer account'a odeme akisi ile aktarilir.
10. Servicing ve IFRS 9/ECL downstream eventleri yayinlanir.

Model cevabi tek basina kredi vermez. Policy, veri kalitesi, affordability,
manual override ve adverse action reason birlikte yonetilir.

### 10.18 Kredi tahakkuk ve geri odeme

1. Daily accrual engine business date ve rate schedule'a gore faiz hesaplar.
2. Hesaplanan accrual deterministik run ID ile ledger'a post edilir.
3. Due date'te principal, interest, fee ve tax receivable olusur.
4. Odeme geldiginde allocation waterfall versiyonlu kurala gore calisir.
5. Kismi odeme her component'in kalanini gunceller.
6. Gecikme aging, delinquency ve collections event'i uretir.
7. Restructure eski schedule'i silmez; amendment ve yeni schedule versiyonu acar.
8. Write-off borcu kaybettirmez; accounting ve collections status ayrilir.

### 10.19 Faiz, ucret, vergi ve gun sonu

1. EOD coordinator legal entity, business date ve dependency readiness kontrolu
   yapar.
2. Cut-off sonrasi yeni islemler yeni business date veya intraday partition'a
   yonlendirilir; tum bankayi zorunlu durdurmak hedef degildir.
3. Interest accrual, capitalization, fee, tax ve dormant account kurallari
   deterministik batch ID ile calisir.
4. Her job restartable, checkpoint'li ve idempotenttir.
5. Control total ve hesap sayisi onceki/sonraki degerlerle raporlanir.
6. Subledger close ve GL interface uretilir.
7. Reconciliation bitmeden period close final olmaz.
8. Hata alan hesaplar exception queue'ya ayrilir; tum batch'in sonucu gizlenmez.

### 10.20 FX donusumlu transfer

1. Quote servisi market rate, spread, fee, kaynak/hedef currency, expiry ve quote
   ID verir.
2. Musteri expiry icinde quote'u kabul eder.
3. Payment quote'u tek kullanim/idempotency ile sahiplenir.
4. Ledger source currency debit, FX bridge pozisyonlari ve target currency credit
   kayitlarini her currency icinde dengeler.
5. Rounding ve realized gain/loss ayri hesaplara gider.
6. Quote expiry veya market closure'da sessiz yeni fiyat uygulanmaz; yeni onay
   istenir.

### 10.21 Sanctions liste guncelleme ve yeniden tarama

1. Guvenilir provider/list source imzali veya dogrulanmis feed saglar.
2. Liste versiyonu, effective time, hash ve ingestion evidence kaydedilir.
3. Normalize/transliteration ve quality kontrolleri yapilir.
4. Yeni liste atomik olarak active version olur.
5. Yeni onboarding ve odemeler active version ile taranir.
6. Mevcut customer/beneficiary portfoyu delta veya tam rescreen edilir.
7. Match otomatik kesin sucluluk sayilmaz; score, reason ve manual case yonetilir.
8. True/false positive karari, reviewer ve evidence degistirilemez tutulur.

### 10.22 AML transaction monitoring

1. Ledger/payment/card events, party risk ve expected activity stream'e gelir.
2. Event-time ve sequence ile eksik/gec gelen veri kontrol edilir.
3. Rules ve modeller structuring, velocity, mule, unusual corridor ve network
   pattern'lerini degerlendirir.
4. Alert ayni senaryo/customer/time window icin dedupe edilir.
5. Case risk onceligine gore investigator'a atanir.
6. Investigator ilgili transaction, party graph ve KYC evidence'i gorur; erisim
   audit edilir.
7. Suspicion karari regulator filing workflow'una gider.
8. Tipping-off nedeniyle musteri bildirimi ve genel support ekranlarinda hassas
   case detayi gorunmez.
9. Filed report, amendment ve authority acknowledgement saklanir.

### 10.23 KYC periyodik ve event-driven review

Tetikleyiciler:

- Risk seviyesine gore review tarihi.
- Adres, meslek, beneficial owner veya direktor degisikligi.
- Sanctions/PEP liste degisikligi.
- Supheli transaction veya beklenen aktiviteden sapma.
- Belge gecerlilik sonu.

Akis:

1. Review case acilir ve gerekli evidence listesi belirlenir.
2. Musteriden yalniz eksik/guncel veri talep edilir.
3. Yeni karar eski karari overwrite etmez; version history tutulur.
4. Restriction gerekiyorsa account/payment policy'ye kontrollu komut gider.
5. Review sonucu ve next-review date yayinlanir.

### 10.24 Reconciliation ve break yonetimi

Reconciliation katmanlari:

1. Journal icinde debit-credit dengesi.
2. Ledger posting ile balance snapshot.
3. Payment state ile ledger journal.
4. Outbox event ile broker yayini.
5. Rail item ile internal payment.
6. Clearing toplamiyla settlement posting'i.
7. Nostro statement ile internal nostro ledger.
8. Subledger toplamiyla General Ledger control account.
9. GL ile finansal/regulator raporu.

Her break su alanlari tasir:

- Kaynak A ve B referanslari.
- Tutar, currency ve fark tipi.
- Ilk gorulme ve aging.
- Materiality/severity.
- Owner ve SLA.
- Otomatik esleme kural versiyonu.
- Manual adjustment onayi ve ledger reference.

Farki kapatmak icin kaydi silmek yasaktir. Adjustment maker-checker ve reason code
ile posting olusturur.

### 10.25 Statement, dekont ve transaction history

1. Ledger committed posting eventleri history projection'ina gelir.
2. Projection aggregateVersion ile sirayi ve duplicate'i kontrol eder.
3. Authorization/pending ile booked hareket farkli gosterilir.
4. Running balance authoritative snapshot ve posting sirasi ile uretilir.
5. Periodic statement legal template, timezone ve business date'e gore kapanir.
6. PDF hash, template version, source snapshot ve generation time ile archive
   edilir.
7. Dekont sadece kanitlanmis transaction verisinden uretilir; istemci request
   alanlari dogrudan dekont olmaz.

### 10.26 Open banking account information

1. TPP FAPI uyumlu authorization baslatir.
2. Musteri consent ekraninda purpose, scope, hesaplar ve sureyi gorur.
3. SCA sonrasi consent active olur.
4. TPP sender-constrained access token alir.
5. API gateway TPP sertifika/client ve quota'yi dogrular.
6. Account API token scope'a ek olarak aktif consent ve hesap listesi kontrol eder.
7. Veri minimizasyonuyla yalniz izinli alanlar dondurulur.
8. Her erisim consent audit'ine yazilir.
9. Revocation sonrasi yeni erisim aninda engellenir; gecmis audit saklanir.

### 10.27 Open banking payment initiation

1. TPP payment consent/intent olusturur.
2. Banka tutar, beneficiary ve risk bilgisini musteriye acik gosterir.
3. SCA transaction detayina dinamik baglanir.
4. Consent/intent bir kez payment command'a donusturulur.
5. Normal payment orchestration, fraud, limit, sanctions ve ledger akisi calisir.
6. TPP polling veya callback ile standart durumlari alir.
7. Callback teslim edilemese bile payment sonucu degismez; tekrar/status API vardir.

### 10.28 Operasyonel admin degisikligi

Ornekler: hesap kisiti, limit override, KYC karar override, fee refund.

1. Calisan phishing-resistant MFA ve privileged access workstation ile girer.
2. Just-in-time rol ve ticket/case baglantisi dogrulanir.
3. Maker komutu olusturur; hassas islem checker tarafindan onaylanir.
4. Policy engine SoD ihlalini engeller.
5. Domain service typed command'i uygular; admin dogrudan DB'ye yazmaz.
6. Onceki/yeni durum, reason, actor, approver ve evidence audit'e gider.
7. Finansal duzeltme varsa immutable adjustment posting olusur.
8. Break-glass kullaniminda anlik alarm ve sonradan zorunlu review vardir.

### 10.29 Bildirim akisi

1. Domain event Notification'a gelir.
2. Preference, legal zorunluluk, sessiz saat ve kanal uygunlugu kontrol edilir.
3. Template event anindaki versiyon ve locale ile render edilir.
4. PII minimum tutulur; push/SMS'e tam hesap veya hassas detay yazilmaz.
5. Provider request benzersiz delivery ID ile gonderilir.
6. Receipt/bounce eventleri delivery state'i gunceller.
7. Retry provider ve hata sinifina gore yapilir.
8. Kritik guvenlik bildirimi pazarlama opt-out'u nedeniyle bastirilmaz.

### 10.30 GL muhasebe ve period close

1. Her operational subledger committed posting'lerden accounting event uretir.
2. Mapping service product, event type, legal entity ve accounting rule version'a
   gore GL hesaplarini belirler.
3. GL interface batch'i control total ile alinir.
4. Duplicate accounting event unique key ile reddedilir.
5. Debit/credit GL journal'i dengeli kaydedilir.
6. Subledger control account toplamiyla GL mutabakati yapilir.
7. Suspense kayitlari owner, aging ve materiality ile takip edilir.
8. Period close, tum kritik feed ve reconciliation tamamlaninca onaylanir.
9. Post-close adjustment ayri period/control ile yapilir; gecmis sessizce acilmaz.

### 10.31 Intraday liquidity ve treasury

1. Payment queue, expected inbound/outbound, nostro bakiye ve collateral akislari
   real-time liquidity platformuna gelir.
2. Her currency ve settlement venue icin mevcut/forecast pozisyon hesaplanir.
3. Limit ve buffer ihlalinde treasury alert/action tetiklenir.
4. Payment prioritization yasal/scheme kurallariyla yapilir; rastgele FIFO yeterli
   olmayabilir.
5. Funding transferi veya merkez bankasi islemi ayri onay ve ledger posting'idir.
6. Gun sonu forecast ile actual settlement farki analiz edilir.

### 10.32 Menkul kiymet islemi ve custody

Opsiyonel yatirim bankaciligi modulu icin:

1. Order suitability, entitlement, market ve pre-trade risk kontrolunden gecer.
2. Execution venue fill mesajlari order'a baglanir.
3. Allocation ve confirmation uretilir.
4. Clearing obligation hesaplanir.
5. Settlement instruction CSD/custodian'a gider.
6. Delivery-versus-payment ile securities ve cash leg birlikte kesinlesir.
7. Position, cash ledger ve custody record mutabik edilir.
8. Corporate action entitlement record date pozisyonundan hesaplanir.

### 10.33 Trade finance - letter of credit

1. Kurumsal musteri application ve belgeleri gonderir.
2. Credit line, collateral, KYC/sanctions, country ve trade risk kontrol edilir.
3. Maker-checker ile issuance onaylanir.
4. SWIFT trade message'i correspondent/advising bankaya gider.
5. Belge presentation document workflow'a gelir.
6. Discrepancy rules ve manual examiner karari kaydedilir.
7. Acceptance/payment halinde limit utilization ve ledger posting yapilir.
8. Fee, commission, contingent liability ve expiry muhasebelesir.

### 10.34 Veri platformuna akis

1. Operational servis outbox/event veya kontrollu CDC uretir.
2. PII siniflandirma ve schema validation ingestion'da uygulanir.
3. Raw zone kaynaga sadik, immutable ve erisim kontrolludur.
4. Conformed zone canonical customer/account/payment kavramlarini ve quality
   kurallarini uygular.
5. Curated mart risk, finance, AML ve urun ihtiyacina gore uretilir.
6. Dataset owner, lineage, freshness, completeness ve reconciliation metrigi
   katalogda tutulur.
7. Duzeltme overwrite degil yeni version/snapshot olarak akar.
8. ML feature'larda training-serving skew ve point-in-time correctness kontrol
   edilir.

### 10.35 Fraud modeli yasam dongusu

1. Confirmed fraud, chargeback ve investigator feedback label store'a gelir.
2. Training dataset zaman penceresi ve lineage ile dondurulur.
3. Model bias, precision/recall, false positive, drift ve stress testten gecer.
4. Model registry approval sonrasi imzali artifact yayimlar.
5. Shadow/canary calisma ile mevcut modelle karsilastirilir.
6. Policy engine hangi segmentte hangi modelin kullanilacagini belirler.
7. Her karar model version ve feature snapshot reference tasir.
8. Kill switch ve onceki modele hizli rollback vardir.

### 10.36 Software deployment akisi

1. Kod review, unit, component, integration ve contract testten gecer.
2. SAST, SCA, secret, IaC, container ve license taramasi yapilir.
3. SBOM ve build provenance uretilir.
4. Artifact imzalanir ve immutable registry'ye konur.
5. GitOps environment promotion yapar; ayni artifact ortamlarda ilerler.
6. Policy gate imzasiz, kritik acikli veya onaysiz image'i engeller.
7. Database migration backward-compatible expand/migrate/contract sirasiyla
   yapilir.
8. Canary/blue-green SLI ve business KPI ile izlenir.
9. Otomatik rollback yalniz veri semantigi guvenliyse kullanilir; finansal event
   semasi geri donusu ayrica planlanir.
10. Deployment, config ve feature flag degisiklikleri audit'e gider.

### 10.37 Incident ve disaster recovery akisi

1. SLI veya control alarmi incident olusturur ve severity siniflar.
2. Incident commander, technical lead, business lead, compliance ve communication
   rolleri atanir.
3. Etkilenen kritik operasyon, musteri, para ve veri kapsami belirlenir.
4. Kill switch/load shedding ile hasar sinirlanir.
5. Ledger/rail belirsiz islemleri quarantine edilir; kor kontrolle tekrar
   gonderilmez.
6. Failover karari runbook ve yetki matrisiyle alinir.
7. DR ortaminda fencing eski writer'i engeller; split-brain olusmaz.
8. Recovery sonrasi backlog kontrollu drain edilir.
9. Ledger, rail, outbox ve projection reconciliation tamamlanir.
10. Regulator/musteri bildirimi hukuki surelere gore yapilir.
11. Blameless post-incident review; root cause, control failure ve aksiyon owner'i
    kaydedilir.
12. Duzeltmeler chaos/DR tatbikatinda tekrar kanitlanir.

## 11. High-Scale Uygulama ve Platform Tasarimi

### 11.1 Kapasite modeli

Kapasite "kac kullanici var" ile hesaplanmaz. Her kritik akis icin su degerler
olculur ve tahmin edilir:

- Normal, peak ve stress TPS.
- P99 payload boyutu.
- Bir command'in urettigi DB write, event ve downstream fan-out.
- Gunluk event hacmi ve retention.
- Hot account/customer/merchant dagilimi.
- Batch window ve cut-off anindaki burst.
- External provider latency ve rate limit.
- Failover halinde tek bolgenin tasimasi gereken yuk.
- Replay/backlog drain sirasinda canli trafige ayrilan kapasite.
- Growth, kampanya, maas gunu ve piyasa stresi katsayilari.

Planlanan kapasite N+1 node degil, bir failure domain kaybindan sonra SLO'yu
karsilayacak kapasitedir. Performans testi ortalama yuku degil peak, skew, retry
storm ve dependency slowdown'u kapsar.

### 11.2 Stateless servis olcekleme

- En az uc replica ve en az uc zone'a topology spread.
- PodDisruptionBudget ve graceful shutdown.
- Readiness yalniz process'in ayakta olmasini degil trafik kabul edebilmesini
  olcer; liveness dependency kesintisinde restart firtinasi yaratmaz.
- CPU yaninda concurrency, request queue, latency ve Kafka lag tabanli autoscale.
- Connection pool, thread pool ve in-flight request sinirlari capacity ile uyumlu.
- Load shedding dusuk oncelikli istegi once reddeder.
- Bounded queue kullanilir; sinirsiz thread/kuyruk yoktur.

### 11.3 Database olcekleme

- Her bounded context kendi schema/database ownership'ine sahiptir.
- Multi-zone synchronous standby, otomatik failover ve fencing.
- Cross-region standby genellikle asynchronous; gercek RPO olculur.
- Read replica yalniz stale read kabul eden sorgularda kullanilir.
- Connection proxy/pool, max connection budget ve query timeout.
- Index, partition, vacuum/statistics ve bloat operasyonu.
- Online schema migration ve backward-compatible deployment.
- WAL archive, encrypted backup, PITR ve restore tatbikati.
- Backup'in varligi degil, hedef RTO icinde geri yuklenmesi kanittir.

Ledger icin read replica'dan available balance karari verilmez. Read-your-writes
gereken akista leader veya session-consistent projection kullanilir.

### 11.4 Kafka/event platformu

- En az uc failure domain'e broker dagilimi.
- Kritik topic'te uygun replication factor ve min in-sync replica.
- Producer `acks=all`, idempotence ve teslim sonucunun izlenmesi.
- Unclean leader election finansal topic'lerde kapali.
- Partition key aggregate/account sirasi gereksinimine gore secilir.
- Consumer concurrency partition sayisiyla planlanir.
- Schema Registry HA ve compatibility policy.
- ACL, TLS, service identity ve topic-level authorization.
- Retention business replay, privacy ve maliyete gore belirlenir.
- Lag yalniz mesaj sayisi degil oldest-event-age ile izlenir.
- Retry topic, exponential delay, max attempt ve DLT ownership.
- Cross-region replication DR icindir; iki cluster'i kontrolsuz ayni aggregate'a
  writer yapmak icin degildir.

### 11.5 Cache

- Cache-aside okuma, bounded TTL ve stampede korumasi.
- Negative cache kisa omurlu.
- Authorization, KYC ve limit cache'i policy'nin kabul ettigi freshness ile
  kullanilir.
- Cache key PII sizdirmaz.
- Cache invalidation event sequence ile stale update'i reddeder.
- Cache kaybinda sistem dogru fakat daha yavas calisabilmelidir.
- Bakiye kaynagi Redis degildir.

### 11.6 Search ve projection

- Projection her event icin aggregateVersion tutar.
- Duplicate ayni version ise no-op.
- Eski version gelirse reddedilir veya gap queue'ya alinir.
- Version gap varsa source replay/snapshot repair istenir.
- Rebuild yeni index/table'a yapilir, tamamlaninca alias atomik degisir.
- Projection freshness API cevabinda veya operasyon metriginde gorunur olabilir.

### 11.7 Multi-region strateji

Tek bir "active-active" etiketi yeterli degildir. Katman bazinda karar gerekir:

- Edge ve stateless read API: active-active olabilir.
- Read projection: her bolgede event replication ile olabilir.
- Identity session: bolgesel veya global store, revoke semantigiyle tasarlanir.
- Ledger write: tek writer per partition veya kanitlanmis consensus; fencing
  zorunlu.
- Payment rail adaptor: rail baglantisi ve message sequence'e gore active-passive
  ya da partitioned active-active.
- Kafka: region-local cluster; DR replication ve controlled promotion.
- Database: region icinde sync, regionlar arasi latency nedeniyle cogu zaman async.

Region failover adimlari:

1. Eski writer'in lease/fence'i kesin olarak iptal edilir.
2. Replication lag ve olasi veri kaybi hesaplanir.
3. Yeni writer promotion yapilir.
4. Traffic weighted olarak acilir.
5. Unknown transaction'lar reconciliation'a alinir.
6. Eski region dondugunde otomatik writer olmaz; resync edilir.

### 11.8 Batch ve streaming birlikte calisma

- Streaming gercek zamanli karar ve projection icindir.
- Batch regulator raporu, EOD, buyuk dosya ve tarihsel yeniden hesap icin kalir.
- Ayni business metrik streaming ve batch'te farkli tanimlanmaz; canonical metric
  catalog kullanilir.
- Batch run ID, input snapshot, code/config version ve output control total tutar.
- Backfill canli topic'e kontrolsuz event basmaz; replay namespace/header ile
  ayrilir.

## 12. Guvenlik Mimarisi

### 12.1 Zero-trust erisim

- Network konumu guven sebebi degildir.
- Her user, device, workload ve servis authenticate edilir.
- Authorization her resource ve action icin policy ile yapilir.
- East-west trafik mTLS ve workload identity tasir.
- Network policy lateral movement'i sinirlar.
- Egress varsayilan kapali; dis hedef allowlist ve egress gateway uzerindendir.
- Policy decision ve enforcement noktalari ayridir; karar audit edilir.

### 12.2 Musteri authentication

- Password varsa modern adaptive hash, breached-password kontrolu ve rate limit.
- Passkey/WebAuthn tercih edilir.
- SMS OTP tek basina yuksek riskli islem icin yeterli kabul edilmez.
- MFA enrollment ve recovery en az login kadar guvenlidir.
- Transaction signing/dynamic linking tutar ve beneficiary'ye baglanir.
- Session fixation, credential stuffing, SIM swap ve phishing sinyalleri
  degerlendirilir.
- Risk bazli step-up acik reason/policy ile uygulanir.

### 12.3 API guvenligi

- FAPI 2.0, OAuth Security BCP ve OIDC uyumlu akis.
- Authorization Code + PKCE; implicit ve password grant yok.
- PAR ve kesin redirect URI kontrolu.
- mTLS veya DPoP sender-constrained token.
- Kisa token omru, dar audience ve scope.
- BOLA/BFLA icin object/action bazli kontrol.
- Request schema, content type, boyut ve recursion limiti.
- SSRF icin URL allowlist, DNS/IP tekrar kontrolu ve metadata endpoint korumasi.
- Rate limit sadece IP degil customer, client, account ve operation bazlidir.
- API inventory, owner, version ve sunset kaydi tutulur.

### 12.4 Cryptography ve anahtar yonetimi

- Root ve payment keys HSM'de.
- Envelope encryption: data key + KMS/HSM master key.
- Ortama ve amaca gore ayri key hierarchy.
- Dual control ve split knowledge.
- Rotation, activation, expiry, revoke ve destruction lifecycle.
- Certificate inventory ve otomatik yenileme.
- Algorithm agility ve quantum-readiness envanteri.
- Anahtar plaintext config, source code veya genel secret'ta tutulmaz.
- Production key'e gelistirici dogrudan erismez.

### 12.5 Secret yonetimi

- Secret manager dynamic/short-lived credential verir.
- Workload identity ile secretless erisim tercih edilir.
- Secret Git, image, Helm values ve logda bulunmaz.
- Rotation uygulama restart'ina mahkum olmaz.
- Database kullanicilari servis ve amac bazinda ayridir.
- Break-glass credential sealed, monitored ve surelidir.

### 12.6 PCI ve kart verisi

- Cardholder Data Environment ayri segmenttir.
- PAN tokenization ve goruntulemede masking.
- CVV authorization sonrasi saklanmaz.
- PIN block yalniz uygun HSM ve PIN security sinirinda islenir.
- P2PE/terminal guvenligi ve key ceremony.
- Card data log, trace, test fixture ve analytics'e girmez.
- Scope ve veri akisi diyagrami periyodik dogrulanir.

### 12.7 Privileged access

- PAM ve just-in-time elevation.
- Kisiye ozel hesap; ortak admin hesabi yok.
- MFA ve hardened admin workstation.
- Session recording ve command audit.
- Maker-checker/SoD.
- Production DB yazma erisimi normal operasyon yolu degildir.
- Break-glass sonrasi otomatik alarm ve review.

### 12.8 Veri koruma

- Transit ve rest encryption.
- Field/token-level protection gerekli alanlarda.
- Purpose limitation ve minimum veri.
- Non-production'da sentetik veya maskeli veri.
- Data loss prevention ve egress kontrolu.
- Retention/legal hold ve dogrulanabilir silme/anonimlestirme.
- Backup ve telemetry de ayni siniflandirma politikasina tabidir.

### 12.9 Threat detection

- IAM, WAF, endpoint, network, HSM, database ve cloud audit SIEM'e akar.
- Kullanici ve entity behavior analytics.
- Detection-as-code ve version control.
- Use-case coverage MITRE ATT&CK ve banka tehdit modeliyle eslenir.
- Alert'in owner, severity, response SLA ve runbook'u vardir.
- SOC aksiyonu finansal sistemde typed/authorized command ile uygulanir.

## 13. Dayaniklilik ve Hata Tasarimi

### 13.1 Failure mode envanteri

Her akis en az su arizalara karsi tasarlanir:

- Client tekrar gonderdi.
- Cevap kayboldu fakat islem tamamlandi.
- Database commit oldu, event yayinlanmadi.
- Event iki kez veya sirasi bozuk geldi.
- Consumer isledi fakat offset commit edemedi.
- External rail cevabi gecikti veya celiskili status verdi.
- Zone, region, DNS, certificate, KMS/HSM veya identity provider kayboldu.
- Clock skew veya business calendar hatasi oldu.
- Schema/config yanlis deploy edildi.
- Downstream yavaslayip thread/connection pool'u tuketti.
- Fraud/KYC provider yok.
- Backup bozuk veya restore beklenenden uzun.
- Insider/admin yanlis komut verdi.

Her failure mode icin detection, containment, customer behavior, retry,
reconciliation, manual repair ve evidence tanimlanir.

### 13.2 Dependency politikasi

Servis dependency manifesti su alanlari tasir:

- Owner ve tier.
- Timeout.
- Idempotency/retry sinifi.
- Circuit breaker esikleri.
- Fallback semantigi.
- Veri freshness toleransi.
- Dependency SLO ve error budget.
- DR dependency ve third-party exit plani.

### 13.3 Graceful degradation

Ornekler:

- Kampanya ve personalization yoksa payment calisir.
- Statement search gecikirse authoritative transaction detail ayri yoldan
  verilebilir.
- Notification provider yoksa kuyruklanir.
- Fraud motoru yoksa dusuk riskli belirli islemler kisitli limitte devam edebilir;
  yuksek riskli transfer fail-closed olur.
- KYC/sanctions unavailable ise yeni onboarding veya ilgili odeme bekler; eski
  cache karari policy'nin izin verdigi sureyle sinirlidir.

Fallback sessiz degildir; metric, audit ve risk tarafindan onayli policy gerekir.

### 13.4 Backup ve restore

- Full + incremental/WAL archive stratejisi.
- Immutable ve ayri hesap/region kopyasi.
- Encryption key backup/recovery proseduru.
- Catalog ve dependency metadata backup'i.
- Periyodik automated restore ve uygulama tutarlilik testi.
- Ledger restore sonrasi posting total, sequence ve external settlement
  reconciliation.
- Ransomware senaryosu ve temiz oda recovery.
- Retention hukuki ve operasyonel gereksinime gore.

### 13.5 Chaos ve resilience testing

- Pod/node/zone kaybi.
- Database failover ve replication lag.
- Kafka broker/partition kaybi.
- Network latency, packet loss ve DNS failure.
- KMS/HSM/IdP kesintisi.
- External payment rail timeout.
- Disk dolmasi ve quota.
- Clock skew.
- Dependency retry storm.
- Corrupt message ve schema incompatibility.
- Region evacuation ve geri donus.

Chaos production'da kontrolsuz ariza cikarmak degildir. Hypothesis, blast radius,
abort condition, owner ve evidence ile yapilir.

## 14. Observability, SRE ve Operasyon

### 14.1 Telemetry standardi

- OpenTelemetry trace, metric ve structured log.
- W3C trace context HTTP/gRPC/Kafka boyunca tasinir.
- Correlation ve causation event envelope'da korunur.
- Service, version, environment, region ve instance resource attribute'lari.
- Log semasi sabittir; JSON olmak tek basina structured log demek degildir.
- PII, credential, token, PAN ve hassas AML verisi telemetry'ye girmez.
- Tail sampling hata, yuksek latency ve yuksek degerli islemleri korur.

### 14.2 Golden signals ve business controls

Teknik:

- Traffic, error, latency, saturation.
- CPU, memory, GC, thread, connection pool.
- DB lock, replication lag, slow query.
- Kafka produce error, ISR, lag ve oldest message age.
- Cache hit/eviction.

Business:

- Payment accepted/completed/rejected/unknown.
- Hold aging ve expired hold.
- Unbalanced journal denemesi.
- Reconciliation break tutari.
- Duplicate/idempotency hit.
- Sanctions/fraud fallback.
- KYC manual review backlog.
- Settlement cut-off riski.
- Suspense account aging.

### 14.3 SLO ve error budget

- SLO musteri tarafindan gozlenen sonucu olcer.
- Planned maintenance otomatik olarak SLO disi sayilmaz; politika aciktir.
- Error budget tuketimi release hizini ve riskli degisiklikleri sinirlar.
- Multi-window burn-rate alert kullanilir.
- Her Tier 0 servis icin sentetik end-to-end probe ve business canary vardir.
- Dependency SLO toplam kullanici SLO'suyla uyumlu tasarlanir.

### 14.4 Incident yonetimi

- 24x7 on-call ve acik escalation.
- Incident severity matrisi.
- Teknik ve business impact ayri raporlanir.
- Status communication ve regulator clock.
- Runbook otomasyonu fakat kontrollu yetki.
- Postmortem ve aksiyonlar issue/risk register'a baglanir.
- Tekrarlayan manuel repair kalici problem kaydina donusur.

## 15. Veri Platformu, Yonetisim ve Yapay Zeka

### 15.1 Data product modeli

Her data product sunlari tanimlar:

- Business owner ve technical owner.
- Semantik tanim ve grain.
- Source ve lineage.
- Sema ve contract.
- Freshness, completeness, accuracy ve reconciliation SLO.
- PII sinifi ve kullanim amaci.
- Retention ve erisim policy.
- Known limitation.

### 15.2 Master ve reference data

- Customer golden record.
- Legal entity ve organization hierarchy.
- Product/reference catalog.
- Currency, country, holiday, BIC, MCC ve code sets.
- Chart of accounts.
- Sanctions/PEP listeleri.

Reference data effective-dated ve versionlu olur. Servisler ayni kod listesini
farkli anlamlarla kopyalamaz.

### 15.3 Data quality

- Completeness.
- Uniqueness.
- Validity.
- Consistency.
- Timeliness/freshness.
- Referential integrity.
- Reconciliation/control total.

Kritik quality ihlali dataset'i quarantine eder veya raporda limitation olarak
gorunur; sessiz null/default ile gizlenmez.

### 15.4 BCBS 239 uyumlu risk verisi

- Board/risk raporu normal ve stres aninda uretilir.
- Manuel aggregation minimumdur.
- Legal entity, risk type, product ve geography kapsami tamdir.
- Kaynaktan rapora lineage kanitlanir.
- Ad hoc kriz sorgusu makul surede uretilir.
- Reconciliation ve limitation raporun parcasi olur.
- Data architecture degisikligi risk reporting etkisiyle review edilir.

### 15.5 ML/AI governance

- Use-case risk siniflandirmasi.
- Training data lineage ve hukuki kullanim amaci.
- Feature definition ve point-in-time correctness.
- Model registry, approval ve imzali artifact.
- Explainability ve adverse action reason.
- Bias/fairness ve segment performansi.
- Drift, calibration ve challenger model.
- Human override ve override monitoring.
- Kill switch ve rollback.
- Prompt/model/agent kullaniminda PII ve tool authorization siniri.
- Uretken AI'ya dogrudan para posting yetkisi verilmez.

## 16. DevSecOps ve Yazilim Tedarik Zinciri

### 16.1 Kod ve mimari kalite kapilari

- Code owner ve zorunlu review.
- Unit, mutation ve property-based test.
- Integration testcontainers.
- Consumer-driven contract ve schema compatibility.
- Architecture-as-code kurallari.
- Performance, soak ve stress test.
- Concurrency ve fault-injection test.
- Financial invariant ve reconciliation test.
- Migration forward/backward compatibility.

### 16.2 Guvenlik kapilari

- Secret scan.
- SAST.
- Dependency/SCA ve license policy.
- SBOM.
- Container ve IaC scan.
- DAST/API security test.
- Threat model ve abuse case.
- Artifact signature ve provenance.
- Critical CVE exception'inda owner, expiry ve compensating control.

### 16.3 Ortam ve release yonetimi

- Production benzeri fakat maskeli/sentetik test verisi.
- Immutable artifact promotion.
- GitOps ve policy-as-code.
- Config schema ve secret reference validation.
- Feature flag owner ve expiry.
- Canary/blue-green.
- Database expand-contract.
- Emergency change icin ayrik hizli ama denetimli yol.
- Release ile business metric korelasyonu.

### 16.4 Test veri stratejisi

- Gercek production verisi gelistirici laptopuna kopyalanmaz.
- Sentetik customer, account, transaction ve fraud senaryolari.
- Deterministik clock ve business calendar.
- Payment rail simulator; approve, reject, timeout, duplicate ve late response.
- HSM/KMS test emulatoru fakat production key yok.
- Golden ledger dataset ve invariant oracle.
- Anonimlestirme geri dondurulemez ve referential consistency korur.

## 17. Organizasyon, Kontrol ve Yonetisim

### 17.1 Uc savunma hatti

- Birinci hat: urun ve operasyon ekipleri riski sahiplenir ve kontrolleri uygular.
- Ikinci hat: risk/compliance bagimsiz politika, challenge ve izleme yapar.
- Ucuncu hat: internal audit bagimsiz assurance verir.

Teknik platform kontrol sahibi olabilir fakat business risk kabulunu tek basina
yapamaz.

### 17.2 Servis sahipligi

Her servis icin:

- Business owner.
- Engineering owner.
- Data owner/steward.
- Risk tier.
- On-call ve escalation.
- SLO ve error budget.
- Dependency ve third-party kaydi.
- RTO/RPO.
- Runbook ve DR kaniti.
- API/event contract ve deprecation.
- Cost/capacity dashboard.

### 17.3 Yeni urun ve degisiklik onayi

- Business case ve customer outcome.
- Legal/regulatory review.
- Financial crime ve fraud assessment.
- Accounting ve tax mapping.
- Data/privacy impact assessment.
- Threat model.
- Operational resilience ve third-party assessment.
- Capacity ve performance.
- Reconciliation/control design.
- Exit/rollback ve decommission plani.

### 17.4 Third-party yonetimi

- Due diligence ve criticality.
- Veri lokasyonu ve subcontractor/fourth-party envanteri.
- SLA/SLO, incident notification ve audit right.
- Encryption/key ownership.
- BCP/DR test kaniti.
- Concentration ve substitutability riski.
- Exit, data portability ve secure deletion.
- Provider outage senaryosu ve local fallback.

## 18. Onerilen Teknoloji Yetkinlikleri

Bu liste marka zorunlulugu degil, gerekli teknik yetkinlikleri ve SpringBank icin
uygun ornekleri gosterir.

| Alan | Onerilen yetkinlik/ornek | Neden |
|---|---|---|
| Uygulama | Java 21 LTS, Spring Boot, modul/hexagonal mimari | Olgun ekosistem, tip guvenligi, observability |
| OLTP | PostgreSQL HA veya kurumsal RDBMS | ACID, constraint, locking, PITR |
| Event | Kafka | Partition ordering, replay, yuksek throughput |
| Outbox | Debezium CDC | DB-event dual-write boslugunu kapatir |
| Event schema | Protobuf/Avro + Schema Registry + AsyncAPI | Tip, evolution ve katalog |
| gRPC | Protobuf + Buf | Dahili typed contract ve breaking gate |
| HTTP | OpenAPI + contract test | Dis/kanal interoperability |
| Workflow | Temporal/Camunda sinifi motor | Timer, insan adimi, uzun saga gorunurlugu |
| Cache | Redis Cluster | Ephemeral cache, rate limit, kisa sureli state |
| Search | OpenSearch/Elasticsearch | Ayrik olceklenen metin/operasyon aramasi |
| Identity | Keycloak sinifi IAM + FAPI sertlestirme | OIDC/OAuth, federation; domain KYC'nin yerine gecmez |
| Workload identity | SPIFFE/SPIRE veya platform esdegeri | IP yerine servis kimligi |
| Secrets | Vault/cloud secrets + KMS/HSM | Dynamic credential ve key lifecycle |
| Runtime | Multi-zone Kubernetes | Scheduling, self-healing, policy ve deployment |
| GitOps | Argo CD/Flux | Declarative, auditli environment state |
| Policy | OPA/Kyverno sinifi policy-as-code | Deployment ve authorization guardrail |
| Telemetry | OpenTelemetry | Vendor-neutral trace/metric/log korelasyonu |
| Metrics/trace/log | Prometheus/Grafana + Tempo/Loki veya SigNoz | SLO ve root-cause gorunurlugu |
| Data | Object storage + Iceberg/Delta/Hudi sinifi lakehouse | Versionlu, buyuk olcekli analitik |
| Stream compute | Kafka Streams/Flink | Stateful real-time fraud/AML/projection |
| Data orchestration | Airflow/Argo Workflows sinifi | Batch dependency, retry ve lineage |
| Catalog | EventCatalog + data catalog | Servis/event/data sahipligi |
| Architecture rule | jQAssistant/ArchUnit | Kod-mimari drift kontrolu |
| Supply chain | SBOM, Sigstore/Cosign, SLSA provenance | Artifact butunlugu ve izlenebilirlik |

### 18.1 SpringBank icin teknoloji karari

- PostgreSQL command store olarak korunabilir; her servis ayri sahiplik ve
  migration ile yonetilmelidir.
- MongoDB read projection icin kullanilabilir; financial source of truth olamaz.
- Kafka event backbone olarak korunabilir; TLS/ACL, HA, schema, outbox/inbox,
  retry/DLT ve replay yonetimi eklenmelidir.
- Keycloak identity provider olarak korunabilir; KYC/party/entitlement domain'leri
  Keycloak attribute'una indirgenmemelidir.
- gRPC dahili anlik komut/sorgularda kullanilabilir; deadline, mTLS, typed error ve
  contract compatibility zorunlu olmalidir.
- EventCatalog ve jQAssistant yasayan mimarinin design-time temelidir.
- OpenTelemetry runtime gercegini eklemelidir.

## 19. Kesinlikle Olmamasi Gerekenler

- Bakiyeyi mutable tek kolon olarak artirip azaltmak.
- Para kaydini update/delete ile duzeltmek.
- Debit ve credit'i iki bagimsiz servis/event ile garantisiz yazmak.
- DB commit ile Kafka send'i koordinesiz dual-write yapmak.
- Kafka future/ack sonucunu yok saymak.
- "Kafka exactly-once acik, problem cozuldu" varsayimi.
- Shared database veya cross-service tablo yazma.
- Controller'in repository/entity'yi dogrudan kullanmasi.
- Persistence entity'sini public API DTO'su yapmak.
- Her alan icin mikroservis acmak veya tum domain'i tek dev servise koymak.
- Uzun senkron servis zinciri kurmak.
- Timeout'suz remote call ve sinirsiz retry.
- Idempotency olmadan finansal POST retry etmek.
- External timeout'u otomatik reject veya success saymak.
- Reconciliation'i raporlama isi gibi ertelemek.
- Redis/search index'i finansal gercek kabul etmek.
- Timestamp ile event sirasi belirlemek; aggregate sequence kullanmamak.
- PII, token, parola, PIN, CVV veya tam request/response loglamak.
- Access token'i URL query'de tasimak.
- Sadece gateway header'ina guvenip ic servis authorization yapmamak.
- Network icini guvenli saymak.
- Plaintext secret ve default sifre kullanmak.
- Production'da otomatik `ddl-auto=update`.
- Tek replica, tek zone, replication factor 1 ile production calistirmak.
- Backup alip restore test etmemek.
- Active-active etiketiyle iki writer'i fencing olmadan acmak.
- Fraud/KYC timeout'unda tanimsiz davranmak.
- Model skorunu aciklama/policy olmadan nihai karar yapmak.
- Admin'e dogrudan database update yetkisi vermek.
- Maker-checker gereken islemi ayni kisiye yaptirmak.
- DLT'yi izlenmeyen mesaj mezarligi yapmak.
- Manuel spreadsheet'i risk veya regulator raporunun ana veri hatti yapmak.
- Event semasini kopya Java DTO'larla servis servis farklilastirmak.
- Islem tamamlanmadan basarili bildirim gondermek.
- Bildirim hatasi icin finansal posting'i rollback etmek.
- Eski business kurallarini overwrite edip tarihsel yeniden uretimi bozmak.
- Tum trafigi loglayip observability elde ettigini sanmak.
- Sadece happy-path unit test ile finansal guvence iddia etmek.

## 20. SpringBank'ten Hedefe Donusum Sirasi

### Faz 0 - Guvenlik ve gercegi gorunur yapma

- Plaintext/default secret'lari kaldir; secret manager ve rotation kur.
- Kafka TLS/ACL, production replication ve min ISR uygula.
- Tum servislerde migration araci ve schema ownership kur.
- OpenTelemetry trace/metric/log correlation ekle.
- Mevcut event semalarini merkezi contract haline getir.
- Tum remote call'lara deadline, retry sinifi ve idempotency tanimla.
- CI'da 13 servis, catalog, contract, security ve architecture gate calistir.

### Faz 1 - Para cekirdegini duzeltme

- Canonical Money value object ve currency kurallarini getir.
- Immutable double-entry ledger kur.
- Hold/reservation ile available/booked balance'i ayir.
- Payment state machine ve idempotency store kur.
- Transactional outbox/inbox uygula.
- Money ownership'ini tek authoritative write path'e indir.
- Reversal, return ve adjustment'i ayri domain olaylari yap.

### Faz 2 - Odeme ve kontrol katmanlari

- Limits reservation/finalization servisi.
- Gercek fraud decision ve feature platformu.
- KYC/AML/sanctions case ve evidence modeli.
- Reconciliation and settlement servisi.
- ISO 20022 canonical model ve rail adaptorleri.
- General Ledger accounting event/mapping.

### Faz 3 - Dayaniklilik ve high scale

- Multi-zone application, database ve Kafka.
- Tier bazli SLO, RTO/RPO ve disruption tolerance.
- Capacity, stress, soak, chaos ve DR testleri.
- Cross-region read ve kontrollu write failover.
- Backlog replay ve reconciliation runbook'lari.
- Third-party concentration/exit planlari.

### Faz 4 - Kurumsal veri ve risk

- Lakehouse, data catalog ve lineage.
- BCBS 239 quality/reconciliation kontrolleri.
- IFRS 9/risk data mart.
- Model governance ve feature store.
- Regulatory reporting evidence ve repeatable snapshot.

## 21. Production Readiness Kabul Listesi

Bir finansal servis production-ready sayilmadan once:

- Veri sahibi ve bounded context'i belli.
- API/event/proto sozlesmesi ve owner'i var.
- Idempotency ve concurrency modeli testli.
- Transaction siniri ve outbox/inbox karari acik.
- Financial invariant/property testleri var.
- Timeout, retry, circuit breaker ve fallback tanimli.
- PII ve threat model review tamam.
- SLO, dashboard, alert ve runbook hazir.
- Capacity ve peak test sonucu var.
- Backup/restore ve DR dependency'si dogrulanmis.
- Reconciliation kontrolu tanimli.
- Migration rollback/forward-fix plani var.
- Audit evidence ve SoD gereksinimi karsilanmis.
- Schema backward compatibility testli.
- DLT/replay owner ve proseduru var.
- Third-party failure davranisi testli.

Bir kritik akis production-ready sayilmadan once:

- Basarili yol.
- Business decline.
- Duplicate request.
- Concurrent request.
- Client timeout.
- Dependency timeout.
- Commit sonrasi cevap kaybi.
- Duplicate/out-of-order event.
- Late external response.
- Partial/unknown state.
- Reversal/return.
- Reconciliation break.
- Zone ve region failure.
- Manual repair ve audit.

senaryolari kanitli testlerden gecmelidir.

## 22. Kaynaklar ve Mimari Gerekceler

### 22.1 Bankacilik ve operasyonel dayaniklilik

- Basel Core Principles, Principle 25: kritik operasyonlari, bunlari destekleyen
  insan, teknoloji, surec, veri, tesis ve ucuncu taraf bagimliliklariyla birlikte
  haritalamayi; disruption boyunca hizmet verebilmeyi bekler:
  https://www.bis.org/committees/bcbs/basel-framework/standard/bcp?allChapters=true
- BCBS Operational Resilience: yonetisim, business continuity, dependency mapping,
  third-party ve incident yonetimi:
  https://www.bis.org/committees/bcbs/basel-consolidated-guidelines/module/orr/20
- BCBS 239: risk data architecture, accuracy, completeness, timeliness ve kriz
  aninda raporlama:
  https://www.bis.org/committees/bcbs/basel-framework/standard/srp/36/inforce/2019-12-15/published/2019-12-15
- DORA: ICT risk, resilience testing, incident ve kritik third-party yonetimi:
  https://eur-lex.europa.eu/eli/reg/2022/2554/oj

### 22.2 Bankacilik domain ve mesaj standartlari

- BIAN Service Landscape: banka kabiliyetlerini service domain olarak ayiran
  referans yapi:
  https://bian.org/deliverables/service-landscape/
- BIAN resmi semantic API repository:
  https://github.com/bian-official/public
- ISO 20022 Repository: ortak finansal data dictionary ve business process
  catalogue:
  https://www.iso20022.org/financial-repository
- ISO 20022 Message Catalogue ve resmi semalar:
  https://www.iso20022.org/catalogue-messages
- CPMI-IOSCO PFMI: liquidity, settlement finality, money settlement ve iletisim
  standardi ilkeleri:
  https://www.bis.org/committees/cpmi/pfmi/overview

### 22.3 Kimlik, API ve veri guvenligi

- NIST SP 800-207 Zero Trust:
  https://csrc.nist.gov/pubs/sp/800/207/final
- NIST SP 800-207A cloud-native workload identity ve uygulama policy modeli:
  https://csrc.nist.gov/pubs/sp/800/207/a/final
- NIST SP 800-63-4 Digital Identity Guidelines:
  https://csrc.nist.gov/pubs/sp/800/63/4/final
- FAPI 2.0 Security Profile: sender-constrained token, PAR, PKCE, mTLS/DPoP ve
  high-value API guvenligi:
  https://openid.net/specs/fapi-security-profile-2_0.html
- OAuth 2.0 Security Best Current Practice, RFC 9700:
  https://datatracker.ietf.org/doc/html/rfc9700
- OWASP API Security Top 10:
  https://api-security.owasp.org/editions/2023/en/0x00-header/
- PCI DSS ve odeme guvenligi dokuman kutuphanesi:
  https://www.pcisecuritystandards.org/document_library/
- PCI payment, PIN, P2PE ve HSM standart ailesi:
  https://www.pcisecuritystandards.org/standards/
- KVKK Kisisel Veri Guvenligi Rehberi:
  https://www.kvkk.gov.tr/SharedFolderServer/CMSFiles/7512d0d4-f345-41cb-bc5b-8d5cf125e3a1.pdf

### 22.4 AML, KYC ve gizlilik

- FATF Recommendations: risk-based CDD, beneficial ownership, wire transfer ve
  suspicious reporting cercevesi:
  https://www.fatf-gafi.org/en/publications/Fatfrecommendations/Fatf-recommendations.html
- FATF Digital Identity Guidance:
  https://www.fatf-gafi.org/content/dam/fatf/documents/recommendations/Guidance-on-Digital-Identity.pdf
- FATF Beneficial Ownership Guidance:
  https://www.fatf-gafi.org/en/publications/Fatfrecommendations/Guidance-Beneficial-Ownership-Transparency-Legal-Arrangements.html
- GDPR Article 5 ve Article 25: purpose limitation, minimization ve privacy by
  design/default:
  https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX:32016R0679

### 22.5 Dagitik sistem, platform ve telemetry

- Apache Kafka design, partition, delivery ve replication semantigi:
  https://kafka.apache.org/40/design/design/
- Debezium Transactional Outbox Event Router:
  https://debezium.io/documentation/reference/stable/transformations/outbox-event-router.html
- PostgreSQL HA/standby:
  https://www.postgresql.org/docs/current/warm-standby.html
- PostgreSQL PITR:
  https://www.postgresql.org/docs/current/continuous-archiving.html
- Kubernetes production environment ve HA:
  https://kubernetes.io/docs/setup/production-environment/
- Kubernetes disruption/PDB:
  https://kubernetes.io/docs/concepts/workloads/pods/disruptions/
- OpenTelemetry signals ve semantic conventions:
  https://opentelemetry.io/docs/concepts/signals/
  https://opentelemetry.io/docs/specs/semconv/

### 22.6 Risk, muhasebe ve secure SDLC

- Basel Framework:
  https://www.bis.org/committees/bcbs/basel-framework
- IFRS 9 Financial Instruments ve expected credit loss:
  https://www.ifrs.org/issued-standards/list-of-standards/ifrs-9-financial-instruments/
- BCBS IRRBB:
  https://www.bis.org/committees/bcbs/basel-framework/standard/srp/98/inforce/2026-01-01/published/2024-07-16
- NIST SP 800-218 Secure Software Development Framework:
  https://csrc.nist.gov/pubs/sp/800/218/final

## 23. SpringBank Gercek Mikroservis ve Veri Akislari

Bu bolum genel bir referans mimari degil, repository'deki controller, service,
gRPC client/server, Kafka listener/publisher ve `application.yaml` dosyalarindan
dogrulanan mevcut SpringBank akisidir. Bir adimin karsisinda `hedef` yazmiyorsa o
adim bugunku kod davranisini anlatir.

### 23.1 Akis okuma kurali

- Dis istek: istemci ile Gateway arasinda HTTP veya deployment TLS sagliyorsa
  HTTPS; repository tek basina edge TLS termination'i kanitlamaz.
- Gateway route: Spring Cloud Gateway istegi path'e gore ilgili servisin HTTP
  portuna iletir.
- Senkron servis cagrisi: Spring gRPC blocking stub kullanilir ve cagiran servis
  cevabi bekler.
- Asenkron servis cagrisi: Kafka topic'i kullanilir; producer HTTP cevabini
  vermeden once tum downstream sonuclari beklemek zorunda degildir.
- Write-side: JPA ve PostgreSQL source of truth'tur.
- Read-side: Kafka projection event'i sonrasinda MongoDB ve Elasticsearch
  eventual-consistent kopyalardir.
- `eventUUID`, `requestId`, `aggregateId` veya Kafka key bir akis korelasyon
  anahtaridir; bunlarin ayni trace/correlation standardinda birlestirilmesi
  hedef mimarinin gerekliligidir.

### 23.2 Calisan servis ve transport haritasi

| Giris veya kaynak | Hedef | Transport | Port/topic | Tasinan ana veri | Hedefteki is |
|---|---|---|---|---|---|
| Istemci | Gateway | REST | `8095` | JWT, request body, query/header | JWT dogrulama, RBAC, route |
| Gateway | user-service | REST | `8081`, `/api/user-service/**` | auth ve user istekleri | Keycloak lifecycle |
| user-service | Keycloak | Keycloak Admin REST | Keycloak server URL | email, sifre, ad, soyad, rol | identity create/update/delete |
| user-service | customer-service | gRPC | `9202` | keycloakId ve profil alanlari | onboarding facade |
| customer-service | customer-service-command | gRPC | `9200` | create/update command | PostgreSQL write |
| customer-service | customer-service-query | gRPC | `9201` | id, keycloakId, filtre | Mongo/Elastic read |
| customer-service-command | customer-service-query | Kafka | `banking-microservices.customer.projection-sync.v1` | customer snapshot event | read model projection |
| user-service | money-service | Kafka | `banking-microservices.user.created.v1` | Keycloak UUID | hesap ve IBAN olusturma |
| money-service | user-service | Kafka | `banking-microservices.money.account.created.v1` | account creation result | duplicate checkpoint |
| Istemci | transaction-service | Gateway + REST | `8083` | transaction command, JWT ve user header'lari | transaction state create |
| transaction-service | user-service | gRPC | `9194` | Authorization token | token decode ve claim donusu |
| transaction-service | money-service | Kafka | `banking-microservices.transaction.created.v1` | transaction DTO | para akisinin baslamasi |
| money-service | transaction-service | Kafka | `banking-microservices.transaction.money-blocked.v1` | block sonucu | transaction status update |
| money-service | user-service | Kafka | `banking-microservices.transaction.user-validation.request.v1` | sender/receiver verisi | kullanici dogrulama |
| user-service | fraud-service ve transaction-service | Kafka | `banking-microservices.transaction.user-validation.success.v1` | zenginlestirilmis user verisi | fraud adimi ve status update |
| fraud-service | money-service | Kafka | `banking-microservices.transaction.fraud.checked.v1` | `FRAUD_REVIEW` durumlu DTO | transfer execution |
| money-service | transaction-service | Kafka | `banking-microservices.transaction.completed.v1` | tamamlanma sonucu | final status update |
| Her transaction parcasi | transaction-service | Kafka | `banking-microservices.transaction.failed.v1` | hata ve status | hata state'i |
| transaction-service | money-service | Kafka | `banking-microservices.transaction.saga.created.v1` | reversal saga | ters para hareketi |
| money-service | transaction-service | Kafka | `...saga.money.completed.v1` veya `...failed.v1` | saga sonucu | saga status update |
| money-service-command | money-service-query | Kafka | `banking-microservices.money.projection-sync.v1` | account snapshot | Mongo/Elastic projection |
| admin-service | admin-service-command | gRPC veya Kafka | `9197` veya `banking-microservices.admin.history.command.v1` | admin request/history | audit write/upsert |
| admin-service-command | admin-service-query | Kafka | `banking-microservices.admin.history.projection-sync.v1` | history snapshot | Mongo/Elastic projection |
| admin-service | admin-service-query | gRPC | `9198` | requestId ve history query | audit history read |

### 23.3 Gateway, JWT ve downstream identity akisi

Ana yol:

Istemci -> Gateway `:8095` -> Keycloak issuer ile JWT validation -> role extraction
-> `X-User-*` header enrichment -> path'e gore hedef mikroservis.

Adimlar:

1. Istemci `Authorization: Bearer ...` ile Gateway'e gelir.
2. Gateway `issuer-uri` olarak `Keycloak /realms/banking` adresini kullanir.
3. `SecurityConfig`, `realm_access.roles` claim'lerini Spring authority'lerine
   cevirir.
4. Public auth path'i haric servis endpoint'leri authenticated olur; admin
   path'leri `ADMIN` rolu ister.
5. `JwtPropertiesFilter`, JWT'den `sub`, email, username, given name, family name
   ve rolleri okur.
6. Gateway downstream istege `X-User-KeycloakUUID`, `X-User-Email`,
   `X-User-Username`, `X-User-Name`, `X-User-Surname` ve `X-User-Roles`
   header'larini ekler.
7. Route path'i `/api/user-service/**`, `/api/money-service/**`,
   `/api/transaction-service/**`, `/api/customer-service/**` veya diger tanimli
   servis path'i ile eslesir ve HTTP istegi ilgili servise gider.

Mevcut risk:

- Gateway istemciden gelen ayni isimli `X-User-*` header'larini once acikca
  silmiyor. Guven siniri, istemci header'ini tamamen kaldirip yalnizca dogrulanmis
  JWT claim'lerinden yeniden uretmelidir.
- Tum command/query servisleri Gateway'de dogrudan route edilmistir. Bu, facade
  sinirini atlama ve internal API'nin disariya acilmasi riskidir.
- Gateway JWT dogrularken transaction-service token'i tekrar user-service'e gRPC
  ile decode ettirir. Imza ve claim dogrulama ile sadece decode etme sorumluluklari
  net ayrilmamistir.

High-scale hedef:

- Edge, gelen identity header'larini strip eder; SPIFFE/mTLS workload identity ve
  imzali internal context kullanir.
- `customer-service-command`, `customer-service-query`, `money-service-command`
  ve benzeri internal servisler public Gateway route'u olmaktan cikarilir.
- Authorization karari sadece header'a guvenmez; policy enforcement point ve
  servis kimligi ile yeniden dogrulanir.
- Her istekte `traceId`, `correlationId`, `causationId`, kanal, cihaz ve risk
  context'i uretilir.

### 23.4 Login, refresh ve logout akisi

Login yolu:

Istemci -> Gateway public auth route -> user-service `POST /v1/auth/login` ->
Keycloak token endpoint -> access/refresh token -> user-service -> Istemci.

1. `AuthController` login DTO'sunu `UserAuthService.login` metoduna verir.
2. `KeycloakUserService`, `client_id`, `client_secret`, password grant, email ve
   sifreyi form-urlencoded olarak Keycloak token endpoint'ine gonderir.
3. Keycloak basariliysa token response user-service tarafindan aynen donulur.
4. `401` cevabi `LoginException`, baglanti hatasi
   `KeycloakConnectionException` olur.

Refresh yolu:

Istemci -> Gateway -> user-service `POST /v1/auth/refresh` -> Keycloak token
endpoint `grant_type=refresh_token` -> yeni token seti -> Istemci.

Logout yolu:

Istemci -> Gateway -> user-service `POST /v1/auth/logout` -> Keycloak logout
endpoint -> refresh token session invalidation -> bos `200` response.

High-scale hedef:

- Password grant kullanilmaz; mobil/web icin Authorization Code + PKCE ve FAPI
  uyumlu akis tercih edilir.
- Client secret browser veya mobil uygulamaya verilmez.
- Refresh token rotation, reuse detection, cihaz/session envanteri ve risk bazli
  step-up uygulanir.
- Login DTO'su veya sifre loglanmaz; mevcut controller/service request loglari
  secret redaction politikasina alinmalidir.

### 23.5 Kullanici kaydi, customer onboarding ve ilk hesap acilisi

Basarili mevcut yol:

Istemci -> Gateway -> user-service -> Keycloak -> customer-service ->
customer-service-command -> customer PostgreSQL -> customer projection Kafka ->
customer-service-query -> MongoDB/Elasticsearch; paralel devamda user-service ->
`user.created.v1` -> money-service -> money PostgreSQL ->
`money.account.created.v1` -> user-service.

Adimlar:

1. Istemci `POST /api/user-service/v1/auth/register` gonderir. Bu Gateway'de
   public route'tur.
2. `AuthController`, `RegisterDto` nesnesini `UserAuthService.register` metoduna
   verir.
3. user-service, Keycloak Admin API'de email tekilligini kontrol eder.
4. Keycloak `banking` realm'inda enabled ve email-verified kullanici olusturur,
   sifreyi atar ve `USER` realm rolunu baglar.
5. Keycloak UUID, user-service'e geri doner. Mevcut tasarimda user-service'in
   ayri bir user PostgreSQL source of truth'i yoktur; identity kaynagi
   Keycloak'tur.
6. user-service, `CustomerOnboardingGrpcClient` ile customer-service `:9202`
   endpoint'ine keycloakId, email, emailVerified, name ve surname gonderir.
7. customer-service onboarding endpoint'i request'i `CustomerService` facade'ina
   verir.
8. Facade, customer-service-command `:9200` gRPC servisine command gonderir.
   Realm `BANKING`, userType `INDIVIDUAL`, status `ACTIVE`, KYC `PENDING`,
   nationality `TR`, risk skorlar `0`, MFA `NONE` ve dil `tr` varsayilir.
9. customer-service-command keycloakId ve email tekilligini, skor araliklarini,
   MFA tutarliligini ve writable kurallarini kontrol eder.
10. Customer entity `banking_customer_command` PostgreSQL veritabanina yazilir.
11. Ayni `@Transactional` method icinde
    `banking-microservices.customer.projection-sync.v1` topic'ine
    `CUSTOMER_CREATED` snapshot event'i gonderilir ve producer sonucu 30 saniyeye
    kadar beklenir.
12. customer-service-query event'i tuketir; daha eski `occurredAt` event'ini
    atlar, MongoDB `banking_customer_query` kaydini ve Elasticsearch customer
    index'ini upsert eder.
13. gRPC response customer-service uzerinden user-service'e customerId,
    keycloakId ve status ile geri gelir.
14. user-service `banking-microservices.user.created.v1` topic'ine Kafka key ve
    payload olarak Keycloak UUID gonderir.
15. money-service duplicate guard sonrasinda `generateUser` ile operasyonel
    `banking_money` PostgreSQL'inde hesap ve IBAN olusturur.
16. money-service `banking-microservices.money.account.created.v1` event'ini
    yayimlar.
17. user-service listener'i eventUUID icin duplicate checkpoint kaydeder. Mevcut
    kodda onboarding aggregate'ini `ACTIVE/COMPLETED` yapan kalici bir registration
    state machine yoktur.
18. HTTP register cagrisi customer gRPC sonucu ve `sendCreateUser` metodunun
    cagrilmasindan sonra `200` doner; money hesabinin gercekten olustugunu
    beklemez.

Hata ve telafi yolu:

- Keycloak create basarisizsa akis customer-service'e hic gitmez.
- customer-service veya customer-command gRPC cagrisi basarisizsa user-service,
  yeni Keycloak hesabini Admin API ile silmeye calisir.
- Keycloak delete de basarisizsa `CustomerRollbackException`, asil exception'a
  suppressed hata olarak eklenir; manuel reconciliation gerekir.
- Customer kaydi basarili, daha sonra `user.created.v1` gonderimi veya money
  hesap acilisi basarisizsa customer kaydini geri alan bir listener/saga yoktur.
- money-service hesap acamazsa
  `banking-microservices.money.account.create-failed.v1` yayimlar; user-service
  konfigurasyonunda topic tanimli olsa da bu hata topic'ini dinleyen listener
  yoktur.
- `KafkaTemplate.send` user-service'te sonucu beklemedigi icin broker tarafindaki
  gecikmeli publish hatasi register try/catch tarafindan gorulmeyebilir.
- Customer DB write ile projection Kafka publish ayni atomik kaynak degildir.
  Publish basarili olduktan sonra DB commit basarisiz olursa hayalet read-model;
  tersi durumda eksik projection olusabilir.

High-scale hedef kayit state machine'i:

`RECEIVED -> IDENTITY_CREATED -> CUSTOMER_CREATED -> ACCOUNT_REQUESTED ->
ACCOUNT_CREATED -> ACTIVE`

Hata state'leri:

`IDENTITY_FAILED`, `CUSTOMER_FAILED`, `ACCOUNT_FAILED`, `COMPENSATION_PENDING`,
`MANUAL_REVIEW`.

Gerekli iyilestirme:

- Her adim tek bir `onboardingId` ve idempotency key tasir.
- Customer ve money write'lari local transaction + outbox ile event uretir.
- Inbox tablosu duplicate event'i kalici olarak engeller.
- Orchestrator timeout sonrasi sonucu query eder; bilinmeyen durumu otomatik
  silme yerine reconciliation'a alir.
- Customer basarili fakat account basarisizsa urun politikasina gore customer
  `ONBOARDING_FAILED` olur veya kontrollu soft-delete compensation calisir.
- Keycloak silme basarisizligi retry queue, alarm ve operasyon case'i uretir.
- API hemen `202 + onboardingId` donup status endpoint'i sunabilir; hesap acilisi
  tamamlanmadan musteriye `ACTIVE` sonucu verilmez.

### 23.6 Customer profil command akisi

Ornek profil guncelleme yolu:

Istemci -> Gateway -> customer-service `PUT /v1/customers/me/profile` ->
customer-service-command gRPC `:9200` -> customer PostgreSQL -> customer
projection Kafka -> customer-service-query -> MongoDB ve Elasticsearch.

1. Gateway JWT'yi dogrular ve Keycloak UUID header'ini ekler.
2. customer-service facade, command DTO'sunu gRPC proto request'ine map eder.
3. customer-service-command customer'i UUID ile bulur ve soft-deleted kaydin
   degistirilmesini engeller.
4. Email degisecekse case-insensitive unique kontrolu yapilir.
5. Ilgili helper yalnizca profil alanlarini gunceller; status, KYC, risk, MFA ve
   special-customer degisiklikleri ayri command metotlaridir.
6. JPA optimistic version ve audit alanlariyla PostgreSQL write yapar.
7. `PROFILE_UPDATED`, `CONTACT_VERIFICATION_UPDATED`, `STATUS_UPDATED`,
   `KYC_STATUS_UPDATED`, `RISK_SCORE_UPDATED`, `MFA_UPDATED`,
   `SPECIAL_CUSTOMER_UPDATED` veya `CUSTOMER_SOFT_DELETED` operation type'i ile
   projection snapshot'i Kafka'ya gider.
8. Query service MongoDB ana read modelini ve Elasticsearch arama dokumanini
   gunceller.
9. Command HTTP cevabi PostgreSQL write sonucundan doner; query read modelinin
   ayni anda guncellenmis olmasi garanti edilmez.

High-scale hedef:

- `If-Match`/version ile lost update semantigi API'ye tasinir.
- Event snapshot yerine gerektiginde semantik domain event ve schema registry
  kullanilir.
- DB + Kafka dual-write transactional outbox olur.
- Mongo update ve Elasticsearch update ayri retry/DLT politikalarina sahip olur;
  birinin hatasi diger basarili projection'i kontrolsuz tekrar ettirmez.
- KYC, risk ve status degisikligi ayricalikli policy, maker-checker ve audit
  evidence ister.

### 23.7 Customer query ve arama akisi

Kendi profilini okuma:

Istemci -> Gateway -> customer-service `GET /v1/customers/me` ->
customer-service-query gRPC `:9201` -> MongoDB -> customer-service -> Istemci.

Admin arama:

Admin istemci -> Gateway RBAC -> customer-service admin endpoint -> query gRPC
-> exact lookup icin MongoDB veya keyword/search icin Elasticsearch -> response.

Mevcut sorgular id, Keycloak UUID, email, telefon, status, KYC status, special
customer ve minimum risk score alanlarini kapsar. Query sonucu projection
gecikmesi kadar eski olabilir. Para transferi gibi guclu tutarlilik isteyen bir
karar bu read model uzerinden verilmemelidir.

High-scale hedef:

- Response `projectionVersion` veya `asOf` bilgisi tasir.
- Read-your-write gereken kanalda command response veya source-of-truth query
  kullanilir.
- Elasticsearch sadece arama kaynagidir; authorization, KYC kararinin kesin
  kaynagi veya bakiye source of truth'i degildir.

### 23.8 Deposit ve withdraw transaction akisi

Ortak giris:

Istemci -> Gateway -> transaction-service `POST /v1/transactions/create` ->
user-service token decode gRPC -> transaction PostgreSQL ->
`transaction.created.v1` -> money-service.

Adimlar:

1. Gateway JWT'yi dogrular ve identity header'larini ekler.
2. transaction-service, Authorization header'ini user-service `:9194` gRPC
   endpoint'ine gonderip token claim'lerini decode eder.
3. transaction-service yeni `eventUUID` uretir, transaction DTO ve entity'yi
   `CREATED` statusu ile kurar.
4. Transaction `banking_transactions` PostgreSQL'ine kaydedilir.
5. Ayni application transaction'i icinde
   `banking-microservices.transaction.created.v1` topic'ine event gonderilir;
   transactional outbox yoktur.
6. money-service event type `DEPOSIT` ise IBAN veya userId ile bakiyeyi artirir.
7. Type `WITHDRAW` ise hesap ve yeterli bakiye kontrolleri sonrasinda bakiyeyi
   azaltir.
8. Bu iki tip user-service validation ve fraud-service'e gitmez.
9. Basarida money-service statusu `COMPLETED` yapip
   `banking-microservices.transaction.completed.v1` yayimlar.
10. transaction-service event'i bulur ve final statusu PostgreSQL'de gunceller.
11. Hatada money-service tip bazli `DEPOSIT_FAILED` veya `WITHDRAW_FAILED`
    statusunu `transaction.failed.v1` topic'ine yollar; transaction-service hata
    state'ini gunceller.

High-scale hedef:

- Deposit ve withdraw dogrudan mutable balance degil, balanced ledger posting
  command'i olmalidir.
- Her para command'i istemci idempotency key'i tasimalidir.
- Transaction kaydi + outbox ayni local commit'te olmalidir.
- Cash deposit'in kaynak kanal, kasa/ATM settlement ve AML kontrolleri;
  withdraw'in limit, step-up, sanction/fraud ve cash availability kontrolleri
  ayri policy adimlaridir.
- `COMPLETED`, ledger posting final olmadan verilmemelidir.

### 23.9 Banka ici transfer akisi

Tam mevcut zincir:

Istemci -> Gateway -> transaction-service -> user-service token gRPC ->
transaction PostgreSQL -> `transaction.created.v1` -> money-service block ->
money PostgreSQL -> `transaction.money-blocked.v1` -> transaction-service ve
`transaction.user-validation.request.v1` -> user-service -> Keycloak ->
`transaction.user-validation.success.v1` -> transaction-service + fraud-service
-> `transaction.fraud.checked.v1` -> money-service execute -> money PostgreSQL ->
`transaction.completed.v1` -> transaction-service.

Adimlar:

1. transaction-service `TRANSFER` request'i icin sender/receiver IBAN, receiver
   name/surname, amount, description, token details ve Gateway header verileriyle
   `CREATED` kaydi olusturur.
2. `transaction.created.v1` event'inin Kafka key'i `eventUUID` olur.
3. money-service sender IBAN'i cozer, receiver userId'yi bulur ve atomik repository
   update'i ile available balance'i azaltip blocked balance'i artirir.
4. money-service ayni DTO'yu iki topic'e yollar:
   `transaction.money-blocked.v1` transaction-service statusu icin;
   `transaction.user-validation.request.v1` user-service validation icin.
5. transaction-service `BLOCK_MONEY` statusunu kaydeder.
6. user-service sender'i Keycloak UUID ile bulur; sender name, surname ve email'i
   DTO'ya yazar.
7. Transfer icin receiver'i Keycloak'ta name + surname ile arar; bulursa receiver
   Keycloak UUID ve email'i DTO'ya yazar.
8. user-service `VALIDATION_PENDING` statuslu
   `transaction.user-validation.success.v1` event'ini yayimlar.
9. transaction-service bu event ile ara statusunu gunceller.
10. fraud-service ayni event'i alir, idempotency kaydi olusturur, statusu
    `FRAUD_REVIEW` yapar ve `transaction.fraud.checked.v1` yayimlar.
11. money-service fraud-checked event'ini alir; blocked balance'dan sender tutarini
    dusurur ve receiver available balance'ina ekler.
12. money-service `transaction.completed.v1` event'ini yayimlar.
13. transaction-service transaction kaydini `COMPLETED` yapar.

Mevcut kritik gercekler:

- fraud-service gercek bir risk modeli, rule sonucu, approve/decline veya manual
  review karari uretmiyor; event'i `FRAUD_REVIEW` statusu ile ileri aktariyor.
- money-service bu event'i onay gibi kabul edip transferi gerceklestiriyor.
- Receiver'i ad + soyad ile Keycloak'ta bulmak benzersiz ve guvenli bir hesap
  resolution yontemi degildir.
- Block islemi sonrasi iki Kafka publish'ten biri basarili, digeri basarisiz
  olabilir; tek atomik outbox batch'i yoktur.
- Para transferi mutable iki balance guncellemesine dayanir; cift tarafli immutable
  ledger yoktur.
- Event producer'larin bir bolumu Kafka future sonucunu beklemez; log'da
  "gonderildi" yazmasi broker acknowledgement kaniti degildir.

High-scale hedef transfer zinciri:

Payment API -> payment-orchestrator -> account/party resolution -> limits reserve
-> sanctions/AML pre-check -> fraud decision -> ledger hold -> posting -> payment
status -> notification -> reconciliation.

Zorunlu farklar:

- Receiver UUID/IBAN ile deterministik cozulur; isim sadece confirmation of payee
  kontrolunde kullanilir.
- Fraud karari `APPROVE`, `DECLINE`, `STEP_UP` veya `MANUAL_REVIEW` ve
  `decisionId/modelVersion/reasonCodes` tasir.
- Hold ve posting ayni ledger invariant'larini kullanir.
- Her event outbox'tan, her consumer inbox/idempotency kaydiyla islenir.
- Timeout, duplicate, out-of-order ve unknown outcome reconciliation ile
  toparlanir.

### 23.10 Transaction iptali ve reversal saga akisi

Giris:

Istemci -> Gateway -> transaction-service `POST /v1/transactions/cancel` ->
transaction PostgreSQL -> tamamlanmamis ise `CANCELLED`; tamamlanmis ise saga ->
money-service -> ters para hareketi -> saga sonucu -> transaction-service.

1. transaction-service `eventUUID` ile islemi bulur.
2. Admin olmayan istekte requester Keycloak UUID ile sender userId eslesmelidir.
3. Zaten `CANCELLED` veya `REVERSED` ise mevcut kayit idempotent sekilde doner.
4. Islem `COMPLETED` degilse status `CANCELLED` olur; para hareketi saga'si
   baslatilmaz.
5. Islem `COMPLETED` ise daha once ayni eventUUID icin saga yoksa `SagaEvents`
   kaydi `banking_transactions` veritabanina yazilir.
6. transaction-service `banking-microservices.transaction.saga.created.v1`
   event'ini yayimlar ve ana transaction'i `REVERSED` olarak isaretler.
7. money-service saga event'ini idempotency kontrolunden gecirip local saga
   kaydini `PROCESS` yapar.
8. Transfer icin receiver'dan cekip sender'a yatirir; deposit icin yatirilani
   geri ceker; withdraw icin cekileni geri yatirir.
9. Basarida `...saga.money.completed.v1`, hatada
   `...saga.money.failed.v1` yayimlanir.
10. transaction-service saga listener'i local saga statusunu gunceller.

Saga ve transaction DTO uyumu - once basit anlatim:

- Ayni Kafka mesaji farkli mikroservislerde farkli alan adi, enum veya anlamla
  tanimlanirsa servisler ayni mesaji farkli yorumlayabilir. Bu, DTO contract
  drift problemidir.
- Cozum, servislerin kendi veritabani entity'lerini ortaklastirmak degil;
  servisler arasinda tasinan mesaj icin tek bir canonical sozlesme belirlemek ve
  her serviste bu sozlesmeyi test etmektir.
- Bu projede saga islemi artik iki serviste de `transaction` olarak adlandirilir
  ve ayni `SagaTransactionSnapshot` anlamini tasir. Eski mesajlar kaybolmasin
  diye onceki alan adlari okunmaya devam eder.
- Transaction eventi icindeki kullanilmayan tum islem-gecmisi listeleri
  kaldirildi. Event yalnizca alicinin gercekten kullandigi snapshot alanlarini
  tasir.

Teknik contract durumu:

- Kafka wire contract'indaki canonical saga alan adi `transaction` olarak
  standartlastirildi.
- transaction-service ve money-service hem yeni `transaction` alanini hem de
  eski `transactionHistory` ve `transactionEntity` alanlarini kabul eder.
- Iki taraftaki snapshot sinif adi `SagaTransactionSnapshot`, alan seti ve alan
  anlamlari aynidir.
- transaction-service, money-service, user-service ve fraud-service icindeki
  `TransactionStatus` degerleri ayni siraya ve anlama getirildi. `CANCELLED` ve
  `REVERSED` artik mesaji tuketen servislerde de tanimlidir.
- Gson ve Jackson annotation'lari birlikte tanimlandigi icin mevcut Gson Kafka
  serializer'i ile kontrollu bir Jackson migrasyonu ayni contract'i korur.
- Dort servisteki contract testleri transaction event alanlarini ve enum
  degerlerini; iki saga testi ise canonical alan adini, snapshot alanlarini ve
  eski alan adlarinin geriye uyumlu okunmasini dogrular.
- Servise ozel `KafkaEventType` ortaklastirilmadi. Bu enum wire contract degil,
  her consumer'in kendi local idempotency durumudur ve kasitli olarak farklidir.
- Ayni wire alanlarini koruyan record/class farki tek basina contract hatasi
  sayilmaz; Java temsilinden cok serilestirilen mesaj semasi esastir.

Uygulanan merkezi contract mimarisi:

- `banking-contracts` ayri Maven artifact'i olarak eklendi.
- `transaction-workflow-v1.proto`, transaction ve saga wire semalarinin tek
  kaynagidir; Java mesaj ve enum siniflari build sirasinda uretilir.
- transaction-service, money-service, user-service ve fraud-service Kafka'ya
  local DTO yerine generated `TransactionWorkflowEvent` veya
  `SagaWorkflowEvent` gonderir.
- Servis icindeki degistirilebilir ara durum `TransactionWorkflowState` olarak
  ayrildi. Bu model wire contract degildir ve Kafka sinirinda mapper ile
  generated contract'a donusturulur.
- Para tutari floating point yerine canonical decimal metin olarak tasinir ve
  servis sinirinda `BigDecimal` olarak kullanilir.
- JWT `tokenDetails` ortak eventten cikarildi; authentication token detayi Kafka
  uzerinden servisler arasinda yayilmaz.
- Listener'lar deploy gecisi ve Kafka retention suresi icin once generated
  contract'i, parse edilemeyen eski mesajlarda legacy Gson modelini okur.
- Root Maven reactor, CI ve ilgili Docker buildleri once contract artifact'ini
  uretir; bagimli servis daha sonra build edilir.
- Sonraki hedef Protobuf Schema Registry compatibility kontrolu ve binary wire
  formatidir. Mevcut adim generated schema sahipligini kurmus, ancak registry
  servisini henuz devreye almamistir.

Devam eden kritik risk:

- Ana transaction, money compensation sonucu gelmeden `REVERSED` yapiliyor.
  Bu nedenle business status gercek para durumunun onune gecebilir.

High-scale hedef:

- Canonical `ReversalRequested` contract tek schema ve contract test ile
  yonetilir.
- Ana transaction once `REVERSAL_PENDING`, money/ledger sonucu geldikten sonra
  `REVERSED` olur.
- Reversal yeni ve bagli bir ledger transaction'idir; eski posting silinmez veya
  mutable balance ile gorunmez kilinmaz.
- Partial reversal, insufficient receiver funds, legal hold ve manual repair
  state'leri ayrica modellenir.
- Saga DB write + event publish outbox ile atomik hale gelir.

### 23.11 Money command/query CQRS akisi

Bu akis mevcut operasyonel `money-service` transaction zincirinden ayridir.

Command yolu:

Istemci -> Gateway -> money-service-command REST ->
`banking_money_command` PostgreSQL -> `money.projection-sync.v1` ->
money-service-query -> MongoDB + Elasticsearch.

1. Command API account create, deposit, withdraw, block-money ve transfer
   endpoint'lerini sunar.
2. PostgreSQL write modeli hesap, available balance ve blocked balance'i tutar.
3. Her write sonrasi account snapshot'li projection event'i Kafka'ya gonderilir.
4. Query listener event `occurredAt` degerini mevcut `lastSyncedAt` ile
   karsilastirip eski event'i atlar.
5. MongoDB `banking_money_query` ana read modelini, Elasticsearch money account
   arama dokumanini tutar.
6. Query API id, userId, IBAN ve search ile okur; gRPC server `:9193` de vardir.

Mevcut risk:

- Bu write modeli ile operasyonel `money-service` farkli PostgreSQL
  veritabanlarinda ayni hesap/bakiye kavramini sahipleniyor. Iki source of truth
  olusmustur.
- Command methodu icinde JPA write ve Kafka publish vardir, outbox yoktur.
- `occurredAt` tek basina ayni aggregate icin guvenli monotonic ordering saglamaz;
  ayni timestamp veya clock skew halinde version gerekir.
- Para arama dokumaninda balance bulundurmak veri sizintisi ve stale decision
  riskini buyutur.

High-scale hedef:

- Tek ledger/account write owner secilir; diger servis facade veya projection
  olur.
- Balance command'lari ledger posting API'sine donusturulur.
- Projection event'i aggregate version tasir ve outbox'tan yayinlanir.
- Elasticsearch balance source'u degil, yalnizca yetkili operasyonel arama
  indeksidir.

### 23.12 Admin operasyonu ve history CQRS akisi

Senkron history yolu:

Admin -> Gateway ADMIN RBAC -> admin-service -> admin-service-command gRPC `:9197`
-> `banking_admin_command` PostgreSQL -> admin projection Kafka ->
admin-service-query -> MongoDB/Elasticsearch.

Asenkron history yolu:

Admin -> Gateway -> admin-service requestId uretir ->
`banking-microservices.admin.history.command.v1` PENDING event'i ->
admin-service-command upsert -> operasyon async calisir -> ayni requestId ile
completion event'i -> command upsert ->
`banking-microservices.admin.history.projection-sync.v1` -> query read model.

Read yolu:

Admin -> Gateway -> admin-service -> admin-service-query gRPC `:9198` ->
MongoDB veya Elasticsearch -> history response.

History kaydi admin email, masked password alani, transport, request type,
target, topic, query text, request/response payload, status, error ve zamanlari
tasir. Password degeri maskeli olsa bile hassas request/response payload'lari
PII ve secret redaction'dan gecmelidir.

High-scale hedef:

- Admin operasyonlari PAM/JIT access, ticket/change id ve maker-checker ile
  iliskilendirilir.
- Audit append-only, WORM/immutable retention ve cryptographic integrity
  kontrolune sahip olur; siradan mutable upsert tek kanit kaynagi olmaz.
- Database query endpoint'i allowlist, read-only credential, row/column policy,
  result limit ve tam audit ile sinirlanir.
- Kubernetes restart/scale gibi aksiyonlar business admin API'sinden ayrilip
  platform control plane ve onayli runbook uzerinden yurutulur.

### 23.13 Akislar arasi ortak bosluklar ve uygulanacak standart

| Alan | Mevcut SpringBank | High-scale banka hedefi |
|---|---|---|
| Kimlik | Keycloak + Gateway JWT, downstream header | FAPI, sender-constrained token, mTLS/workload identity, header stripping |
| Sync servis iletisimi | Blocking gRPC, sinirli timeout politikasi | Deadline, retry budget, circuit breaker, mTLS, contract compatibility |
| Async iletisim | Kafka + JSON/Gson/Jackson karisimi | Canonical envelope, schema registry, compatibility gate, outbox/inbox |
| Para modeli | Mutable available/blocked balance | Immutable double-entry ledger + derived balance |
| Fraud | Status forwarding | Rule/model decision, reason code, model version, manual review |
| CQRS ordering | `occurredAt` stale check | Aggregate version, partition key, idempotent projection, replay |
| Onboarding | Senkron Keycloak/customer, async money | Persistent saga/state machine + reconciliation + case management |
| Reversal | Mutable ters bakiye islemleri | Bagli compensating ledger transaction, pending/final states |
| Audit | Application log ve history projection | Tamper-evident audit, retention, access review, evidence chain |
| Observability | Servis loglari ve actuator | End-to-end trace, correlation/causation, SLI, business control dashboard |

Tum yeni akislar icin zorunlu tasarim sablonu:

1. Giris endpoint'i, caller ve authentication kaynagi.
2. Command sahibi ve tek source of truth.
3. Senkron cagrilarin deadline, retry ve idempotency davranisi.
4. Local transaction siniri.
5. Outbox event adi, schema owner'i, partition key ve version.
6. Consumer inbox, duplicate ve out-of-order davranisi.
7. Basari, decline, timeout, unknown ve manual-review state'leri.
8. Compensation/reversal ve reconciliation sahibi.
9. PII, secret, audit ve retention sinifi.
10. Trace, metric, alert, SLO ve runbook baglantisi.

Bu sablon doldurulmadan yeni bir mikroservis veya yeni bir Kafka topic'i
eklenmemelidir. Once mevcut akis ve sahiplik kontrol edilir; var olan akis varken
ayni sorumluluk icin ikinci bir akis kurulmaz.

## 24. Son Mimari Karar

Profesyonel high-scale bankaciligin merkezi "mikroservis sayisi" degildir.
Merkezde su dort kanit bulunur:

1. Her para hareketi immutable, dengeli ve yeniden uretilebilir ledger kaydidir.
2. Her dagitik akis idempotent, versionlu, gozlemlenebilir ve reconciliation ile
   toparlanabilir.
3. Her kritik operasyon failure domain, third-party ve insan bagimliliklariyla
   birlikte disruption boyunca hizmet verecek sekilde tasarlanmistir.
4. Her karar kimlik, policy/model versiyonu, veri kaynagi ve audit evidence ile
   aciklanabilir.

SpringBank'in hedefi once bu dogruluk ve kontrol omurgasini kurmak, sonra trafik
olcegini buyutmektir. Dogrulugu kanitlanmamis bir para modelini yatay olceklendirmek
yalnizca hatayi daha hizli ve daha genis yayar.
