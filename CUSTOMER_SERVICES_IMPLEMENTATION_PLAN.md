# Customer Services Implementation Plan

Bu dokuman kod icermeyen uygulama planidir.
Amac, customer-service, customer-service-command ve customer-service-query mikroservisleri yazilmadan once mevcut proje akislarini kesinlestirmek, yeni servislerin sorumluluklarini belirlemek ve ileride kodun guncellenebilir kalmasi icin alinacak kararlari netlestirmektir.

## Temel Kural

Var olan akis varken yeni paralel akis kurulmayacak.
Customer olusturma, mevcut create user akisini genisletecek.
Yeni customer servisleri, mevcut money/admin command-query yapisina benzer sekilde yazilacak.

## Mevcut Create User Akisi

Projede zaten user create akisi var.
Bu akisin merkezi user-service icindeki register operasyonudur.

Mevcut akis:

1. Gateway register istegini user-service auth endpointine yonlendirir.
2. user-service register istegini alir.
3. Keycloak uzerinde kullanici olusturulur.
4. Keycloak kullanici id bilgisi user-service tarafina doner.
5. user-service Kafka uzerinden create-user eventi yayinlar.
6. money-service bu eventi dinler.
7. money-service kullanici icin para hesabi ve IBAN olusturur.
8. money-service basari veya hata eventini tekrar Kafka'ya basar.
9. user-service create-user sonuc eventini dinleyerek duplicate/islenmis event bilgisini saklar.

Customer entegrasyonu bu akisi bastan kurmayacak.
Keycloak olusturma adimi ayni kalacak.
Customer kaydi Keycloak olustuktan sonra, money account eventinden once zorunlu bir onboarding adimi olarak eklenecek.

## Yeni Customer Create Akisi

Kesinlesen hedef akis:

1. Register istegi gateway uzerinden user-service tarafina gelir.
2. user-service Keycloak uzerinde kullaniciyi olusturur.
3. user-service, customer-service facade servisine gRPC ile customer create istegi gonderir.
4. customer-service bu istegi alir ve customer-service-command servisine gRPC ile iletir.
5. customer-service-command customer kaydini PostgreSQL write database'e yazar.
6. customer-service-command basarili write sonrasi customer projection eventini Kafka'ya basar.
7. customer-service-query projection eventini dinler.
8. customer-service-query MongoDB read modelini ve Elasticsearch indexini gunceller.
9. customer-service-command basari cevabini customer-service'e doner.
10. customer-service basari cevabini user-service'e doner.
11. user-service mevcut create-user Kafka eventini yayinlar.
12. money-service mevcut sekilde money account olusturur.

Bu siralama bilincli secildi.
Customer kaydi basarisiz olursa money account create eventi yayinlanmayacak.
Boylece Keycloak kullanicisi olan ama customer profili olmayan, uzerine para hesabi acilmis yarim bir kullanici olusmayacak.

## Customer Create Hata ve Rollback Akisi

Customer olusturma basarisiz olursa:

1. user-service hatayi yakalar.
2. user-service Keycloak'ta yeni olusturulan kullaniciyi siler.
3. rollback sonucu loglanir.
4. rollback de basarisiz olursa asil hata korunur, rollback hatasi da operasyonel loga yazilir.
5. register endpoint kullaniciya basarisiz cevap doner.
6. money account create eventi yayinlanmaz.

Bu davranis compensation mantigidir.
Burada Kafka saga yerine senkron gRPC compensation tercih edilecek cunku customer create, register operasyonunun zorunlu parcasi olacak.

## Customer Service Sorumlulugu

customer-service facade/gateway arkasi domain servisi gibi davranacak.
Kendi source-of-truth database'i olmayacak.
Command ve query servisleriyle gRPC uzerinden alisveris yapacak.

Sorumluluklari:

- user-service tarafindan gelen create customer istegini kabul etmek.
- customer-service-command servisine gRPC command cagrisi yapmak.
- customer-service-query servisine gRPC query cagrisi yapmak.
- Public/user ve admin endpointleri ayirmak.
- Request validation, response mapping ve loglama yapmak.
- Command/query servislerinin teknik detaylarini dis API'den gizlemek.

customer-service REST endpointleri gateway arkasinda calisacak.
Admin endpointler gateway role kontrolu altinda olacak.

## Customer Service Command Sorumlulugu

customer-service-command write-side servistir.
Source of truth PostgreSQL olacak.

Sorumluluklari:

- Customer entity kaydini olusturmak.
- Customer profil bilgilerini guncellemek.
- Customer status degistirmek.
- KYC status degistirmek.
- Risk score ve special customer score guncellemek.
- MFA tercihini guncellemek.
- Soft delete yapmak.
- Her basarili write sonrasi projection event yayinlamak.
- Duplicate keycloak id veya email durumlarini engellemek.
- Optimistic locking ile lost update riskini azaltmak.

Bu servis MongoDB veya Elasticsearch'e direkt yazmayacak.
Sadece PostgreSQL'e yazacak ve Kafka projection eventi yayinlayacak.

## Customer Service Query Sorumlulugu

customer-service-query read-side servistir.
MongoDB ana read model, Elasticsearch arama indexi olacak.

Sorumluluklari:

- customer projection eventlerini Kafka'dan dinlemek.
- MongoDB customer read modelini guncellemek.
- Elasticsearch customer indexini guncellemek.
- id, keycloakId, email ve phoneNumber ile hizli sorgu sunmak.
- name, surname, email, phoneNumber, nationalityCode, status, kycStatus gibi alanlarda search/list endpointleri sunmak.
- Stale event kontrolu yapmak.
- gRPC query endpointleri sunmak.

Bu servis PostgreSQL write database'e gitmeyecek.

## Ana Model Kararlari

Ana entity customer-service-command icinde olacak.
Model, kullanicinin verdigi BaseEntity'den extends alacak.
BaseEntity auditing, version, soft delete ve UUID id standardini saglayacak.

Customer modelinde bulunacak ana alanlar:

- keycloakId
- realm
- email
- emailVerified
- phoneNumber
- phoneVerified
- userType
- status
- birthdate
- name
- middleName
- surname
- sex
- riskScore
- mfaEnabled
- mfaMethod
- preferredLanguage
- specialCustomer
- specialCustomerScore
- kycStatus
- nationalityCode

Alan kararlari:

- keycloakId UUID tipinde olacak.
- id alanini BaseEntity saglayacak.
- email unique olacak.
- keycloakId unique olacak.
- phoneNumber bos olabilir ama doluysa query icin indexlenebilir olacak.
- riskScore ve specialCustomerScore negatif olamayacak.
- preferredLanguage varsayilan olarak tr olacak.
- status varsayilan olarak ACTIVE olacak.
- kycStatus varsayilan olarak PENDING olacak.
- mfaMethod varsayilan olarak NONE olacak.
- specialCustomer boolean olacak ve varsayilan false olacak.

## Enum Kararlari

realm enum:

- BANKING

userType enum:

- INDIVIDUAL
- CORPORATE
- STAFF
- ADMIN

userStatus enum:

- ACTIVE
- PASSIVE
- SUSPENDED
- CLOSED

sex enum:

- MALE
- FEMALE
- OTHER
- UNSPECIFIED

mfaMethod enum:

- NONE
- SMS
- EMAIL
- AUTHENTICATOR

kycStatus enum:

- PENDING
- APPROVED
- REJECTED

nationalityCode enum:

- TR
- US
- DE
- GB
- FR
- OTHER

Bu enum listesi ilk surum icin yeterli tutulacak.
Ileride genisleyebilecek enumlar icin response ve projection tarafinda string temsili korunacak.

## Command Operasyonlari

customer-service-command tarafinda ilk surumde su command operasyonlari olacak:

- create customer
- update profile
- update contact verification
- update status
- update kyc status
- update risk score
- update mfa preference
- mark special customer
- soft delete customer

Bu operasyonlar helper/service parcalarina ayrilacak.
Tek bir service class icine tum is mantigi yigilmayacak.

Planlanan helper ayrimi:

- CustomerValidator
- CustomerMapper

- CustomerStatusService
- CustomerRiskService
- CustomerKycService
- CustomerMfaService
- CustomerProjectionEventFactory
- CustomerDeletionService

## Query Operasyonlari

customer-service-query tarafinda ilk surumde su query operasyonlari olacak:

- get by id
- get by keycloak id
- get by email
- get by phone number
- search by keyword
- list by status
- list by kyc status
- list special customers
- list high risk customers

Read modeli MongoDB'den okunacak.
Arama icin Elasticsearch indexi guncellenecek.
Basit endpointler once MongoDB repository pattern ile yazilacak; Elasticsearch indexleme projection tarafinda hazir olacak.

## Facade Controller Operasyonlari

customer-service tarafinda kullanici ve admin operasyonlari ayrilacak.

User-facing endpointler:

- kendi customer profilini getirme
- kendi iletisim bilgilerini guncelleme
- kendi MFA tercihini guncelleme

Admin endpointler:

- customer get by id
- customer get by keycloak id
- customer search
- customer status update
- KYC status update
- risk score update
- special customer update
- soft delete

Create customer endpointi de olacak, fakat user-service register akisi esas kullanim noktasi olacak.
Bu endpoint test, admin operasyon veya internal ihtiyaclar icin kontrollu sekilde tutulabilir.

## gRPC Kararlari

customer-service ile customer-service-command arasinda command gRPC olacak.
customer-service ile customer-service-query arasinda query gRPC olacak.
user-service ile customer-service arasinda onboarding gRPC olacak.

gRPC kullanilacak ana senaryolar:

- user-service customer create cagrisi
- customer-service command create/update/status/risk/KYC cagrilari
- customer-service query get/search cagrilari

gRPC hatalari domain exception'a map edilecek.
NOT_FOUND, ALREADY_EXISTS, INVALID_ARGUMENT ve INTERNAL statusleri ayrilacak.

## Kafka Projection Akisi

customer-service-command her basarili state degisikliginde projection event yayinlayacak.
customer-service-query bu eventleri dinleyip read model olusturacak.

Topic karari:

- banking-microservices.customer.projection-sync.v1

Projection event icerigi:

- eventId
- aggregateId
- keycloakId
- realm
- email
- emailVerified
- phoneNumber
- phoneVerified
- userType
- status
- birthdate
- name
- middleName
- surname
- sex
- riskScore
- mfaEnabled
- mfaMethod
- preferredLanguage
- specialCustomer
- specialCustomerScore
- kycStatus
- nationalityCode
- operationType
- occurredAt
- sourceService

operationType degerleri:

- CUSTOMER_CREATED
- PROFILE_UPDATED
- CONTACT_VERIFICATION_UPDATED
- STATUS_UPDATED
- KYC_STATUS_UPDATED
- RISK_SCORE_UPDATED
- MFA_UPDATED
- SPECIAL_CUSTOMER_UPDATED
- CUSTOMER_SOFT_DELETED

## Database Kararlari

customer-service-command:

- PostgreSQL kullanacak.
- Database adi banking_customer_command olacak.
- Ana tablo customers olacak.
- keycloakId ve email unique constraint alacak.
- status, kycStatus, userType ve deleted alanlari query performansi icin indexlenebilir olacak.

customer-service-query:

- MongoDB kullanacak.
- Database adi banking_customer_query olacak.
- Collection adi customer_profiles olacak.
- Elasticsearch index adi customer-profiles olacak.

customer-service:

- Kendi database'i olmayacak.
- Facade olarak command/query servisleriyle gRPC uzerinden calisacak.

## User Service Entegrasyonu

user-service register davranisi genisletilecek.

Yeni davranis:

1. Keycloak kullanicisi olusturulur.
2. Customer create gRPC cagrisi yapilir.
3. Customer create basariliysa mevcut Kafka create-user eventi yayinlanir.
4. Customer create basarisizsa Keycloak kullanicisi silinir.
5. Hata register operasyonuna doner.

Bu noktada mevcut money-service create-user Kafka akisi korunacak.
Money account create eventi customer basarisindan sonra yayinlanacak.

user-service icine customer gRPC client eklenecek.
Keycloak rollback icin mevcut deleteUserById metodu kullanilacak.
Rollback hatasi asil hatayi ezmeyecek.

## Exception Kararlari

Her customer servisinde GlobalExceptionHandler olacak.

customer-service-command exceptionlari:

- CustomerCommandException
- CustomerAlreadyExistsException
- CustomerNotFoundException
- InvalidCustomerStateException
- InvalidRiskScoreException
- InvalidKycTransitionException
- ProjectionPublishException

customer-service-query exceptionlari:

- CustomerQueryException
- ReadModelNotFoundException
- ProjectionSyncException

customer-service exceptionlari:

- CustomerServiceException
- CustomerCommandClientException
- CustomerQueryClientException
- CustomerOnboardingException

user-service entegrasyon exceptionlari:

- CustomerOnboardingException
- CustomerRollbackException

## Loglama Kararlari

Loglar mevcut proje stiline benzer olacak.

Loglanacak ana noktalar:

- register akisi icinde Keycloak create basarisi
- customer create gRPC istegi
- customer command write basarisi
- projection event publish basarisi
- query projection consume basarisi
- customer create failure
- Keycloak rollback basarisi
- Keycloak rollback failure
- duplicate customer denemeleri
- status, KYC, risk ve MFA degisimleri

Sensitive bilgi loglanmayacak.
Password, token ve full claim bilgisi loglara yazilmayacak.

## Validation Kararlari

Ilk surum validationlari:

- email bos olamaz.
- keycloakId bos olamaz.
- name bos olamaz.
- surname bos olamaz.
- riskScore 0 ile 100 arasinda olmali.
- specialCustomerScore 0 ile 100 arasinda olmali.
- preferredLanguage bos olamaz.
- kycStatus null olamaz.
- userType null olamaz.
- status null olamaz.
- mfaEnabled false ise mfaMethod NONE olmali.
- mfaEnabled true ise mfaMethod NONE olmamali.
- deleted customer uzerinde kritik update engellenmeli.

## Test Plani

Ilk implementasyonda eklenecek testler:

- customer create basarili oldugunda projection event yayinlanir.
- duplicate email reddedilir.
- duplicate keycloakId reddedilir.
- riskScore invalid ise hata doner.
- MFA method ve mfaEnabled uyumsuzsa hata doner.
- customer query read model yoksa not found doner.
- projection stale event geldiyse read model geriye sarilmaz.
- user-service register akisinda customer create basarisizsa Keycloak rollback calisir.
- Keycloak rollback basarisiz olsa bile asil customer create hatasi korunur.

## Kubernetes ve Gateway Kararlari

Yeni servisler k8s uygulama manifestine eklenecek.

Port kararları:

- customer-service HTTP portu 8099
- customer-service-command HTTP portu 8100
- customer-service-command gRPC portu 9200
- customer-service-query HTTP portu 8101
- customer-service-query gRPC portu 9201
- customer-service gRPC portu 9202

Gateway route kararları:

- customer-service route eklenecek.
- customer-service-command route sadece admin/internal gerekirse eklenecek.
- customer-service-query route query ihtiyaci icin eklenecek.
- admin pathleri ADMIN role gerektirecek.
- user-facing customer pathleri authenticated olacak.

PostgreSQL init database listesine banking_customer_command eklenecek.
MongoDB icin banking_customer_query kullanilacak.
Elasticsearch icin customer-profiles indexi projection sirasinda olusacak.

## Docker ve Build Kararlari

Her yeni servis icin Dockerfile eklenecek.
Mevcut servislerdeki Maven build pattern'i takip edilecek.
Java 21 kullanilacak.
Spring Boot surumu proje standardina gore mevcut command/query servisleriyle ayni tutulacak.

## Guncellenebilirlik Onerileri

Bu feature ile birlikte su eklemeler mantikli:

- Customer projection eventlerinde event version alani eklenebilir.
- Customer servislerinde correlationId header tasinabilir.
- Register akisi icin onboarding status tablosu ileride eklenebilir.
- Customer KYC ve risk skor degisimleri admin history tarafina audit event olarak baglanabilir.
- Customer search icin Elasticsearch sorgusu ikinci fazda daha aktif kullanilabilir.
- Phone number icin ulke kodu ve normalize edilmis alan ileride ayrilabilir.
- Nationality enum yerine ISO country code tablosu ileride daha dogru olabilir.
- PII alanlari icin masking ve log sanitizer ortak utility haline getirilebilir.
- Soft delete olan customer icin restore operasyonu admin tarafina eklenebilir.
- Customer create basarisizligi cok kritik hale gelirse register compensation saga kaydi eklenebilir.

## Simdilik Sorulmayacak Kararlar

Bu kararlar mevcut pattern'e gore bariz oldugu icin implementasyonda tekrar sorulmayacak:

- command database PostgreSQL olacak.
- query database MongoDB ve Elasticsearch olacak.
- customer-service facade olacak.
- user-service register akisi korunacak.
- Keycloak rollback user-service tarafinda yapilacak.
- projection event Kafka ile tasinacak.
- GlobalExceptionHandler her serviste olacak.
- DTO, repository, exception, service ve controller katmanlari otomatik olusturulacak.

## Sadece Gerekirse Sorulacak Kararlar

Implementasyona gecmeden veya gecis sirasinda sadece su durumlarda soru sorulacak:

- Customer create basarisiz oldugunda money account daha once olusmus olursa ayrica compensation istenecek mi?
- Register DTO icine phoneNumber, birthdate, middleName gibi alanlari hemen ekleyelim mi, yoksa customer create ilk fazda mevcut email/name/surname ile mi calissin?
- customer-service-command ve customer-service-query endpointleri gateway uzerinden disariya acilsin mi, yoksa sadece customer-service facade public olsun mu?
- KYC ve risk update operasyonlari ilk fazda sadece admin endpointlerde mi olsun?

Bu sorular gercek API davranisini ve veri akisini degistirdigi icin anlamli sorulardir.
Bariz kod kalite detaylari icin ayrica soru sorulmayacak.

## Uygulama Sirasi

1. customer-service-command iskeleti olusturulacak.
2. BaseEntity ve Customer entity yazilacak.
3. enumlar, DTO'lar, repository ve exceptionlar yazilacak.
4. command service ve helper servisleri yazilacak.
5. command gRPC endpoint ve projection publisher yazilacak.
6. customer-service-query iskeleti olusturulacak.
7. read document, search document, repository, projection listener ve query service yazilacak.
8. query gRPC endpoint ve REST query controller yazilacak.
9. customer-service facade iskeleti olusturulacak.
10. command/query gRPC clientlari, service ve controllerlar yazilacak.
11. user-service icine customer onboarding gRPC client eklenecek.
12. register akisi customer create ve Keycloak rollback ile genisletilecek.
13. gateway route ve k8s manifestleri guncellenecek.
14. Dockerfile ve application.yaml dosyalari tamamlanacak.
15. Unit testler eklenecek.
16. Maven test/compile kontrolu calistirilacak.

## Kesinlesen Sonuc

Customer servisleri mevcut proje mimarisine uygun olarak uc parca halinde kurulacak.
Yeni bir create-user akisi icat edilmeyecek.
Var olan register akisi genisletilecek.
Command tarafinda PostgreSQL, query tarafinda MongoDB ve Elasticsearch kullanilacak.
Servisler arasi customer command/query haberlesmesi gRPC ile yapilacak.
Projection sync Kafka ile akacak.
Customer create register icin zorunlu adim olacak.
Customer create basarisiz olursa Keycloak kullanicisi rollback ile silinecek.

## Uygulama Sonrasi Dogrulama

Planlanan customer servisleri uygulanmistir.

- customer-service-command PostgreSQL write modeli, BaseEntity audit alanlari, optimistic locking, soft delete, domain enumlari, repository, validation, helper servisleri, command REST ve gRPC endpointleri ile calisir.
- customer-service-query Kafka projectionlarini stale event kontroluyle MongoDB ve Elasticsearch tarafina yansitir; read REST ve gRPC endpointleri sunar.
- customer-service facade command ve query servisleriyle gRPC uzerinden haberlesir; user endpointleri ile admin endpointlerini ayirir.
- user-service register akisi Keycloak sonrasinda customer onboarding gRPC cagrisi yapar. Onboarding basarisizsa Keycloak kullanicisini siler ve mevcut money account Kafka eventini yayinlamaz.
- Kullanici kendi profilini veya MFA ayarini guncellerken customer id sahipligi Keycloak kimligiyle dogrulanir.
- Command, query ve onboarding protobuf contractlari iki ucta wire semasi olarak ayni tutulur.

## Sonraki Mantikli Iyilestirmeler

- Command write ve Kafka publish arasindaki dagitik tutarliligi daha da guclendirmek icin transactional outbox ve retry/DLQ stratejisi eklenebilir.
- Email veya telefon profilde degistiginde Keycloak ile customer domaini arasinda hangi sistemin master oldugu kesinlestirilmeli; gerekirse kontrollu senkronizasyon akisi kurulmalidir.
- Onboarding cagrisi response kaybi sonrasi tekrarlandiginda idempotent sonuc donmesi icin keycloakId bazli create sonucu ve retry semantigi guclendirilebilir.
- KYC kararlarinin detayli audit gecmisi, belge referanslari, karar veren kullanici ve karar nedeni ayri immutable bir audit modeliyle tutulabilir.
- Customer PII alanlari icin field-level encryption, maskeleme politikasi ve log redaction standardi planlanmalidir.
