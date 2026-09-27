# SpringBank Veri Akisi ve Servis Sorumluluklari

## Amac ve Kapsam

Bu belge, projenin mevcut kaynak kodu, servis ayarlari, gRPC kontratlari, Kafka topic tanimlari ve Kubernetes dagitimi incelenerek hazirlandi. Hedef, yeni bir ozellik eklenirken tahmini bir mimari yerine bugun sistemde calisan gercek veri akisini kullanmaktir.

Akislarin Draw.io gorseli `readme/current-data-flow-architecture.drawio` dosyasindadir. Dosya bes sayfada genel servis haritasini, kullanici kaydini, transaction/saga akisini, CQRS-admin zincirini ve Gateway-Keycloak identity akisini gosterir. Ok renkleri kullanilan tasima mekanizmasini belirtir.

Kurumsal hedef mimari, high-scale banka akislarinin tam listesi ve mevcut
SpringBank akislarinin hedef karsiliklari
`HIGH_SCALE_BANKING_REFERENCE_ARCHITECTURE.md` belgesindedir. Ozellikle bu
belgenin 23. bolumu; endpoint, mikroservis, gRPC portu, Kafka topic'i, veritabani,
cevap ve hata/telafi yolunu hop-by-hop olarak bir arada verir.

Bu nedenle iki kavram ayri tutulur:

- Senkron akis: Istegi yapan taraf, sonraki adimin sonucunu bekler. REST ve gRPC bu gruptadir.
- Asenkron akis: Uretici Kafka'ya olayi birakir; tuketici daha sonra kendi verisini gunceller. CQRS read model senkronizasyonu ve islem sagalari bu gruptadir.

Kaynakta gorunen her servisin kendi veri siniri vardir. Bir servisin veritabani diger servis tarafindan dogrudan kullanilmamalidir.

## Platforma Giris Noktasi

Tum istemci istekleri once `gateway` servisine gelir. Gateway, `8095` portunda dis dunyaya acik tek HTTP giris katmanidir.

| Katman | Sorumluluk | Veri veya karar |
| --- | --- | --- |
| Gateway | Route secimi, JWT dogrulama, rol kontrolu ve header zenginlestirme | Istegi ilgili mikroservise iletir; is verisi sahiplenmez |
| Keycloak | Kimlik, sifre, oturum, JWT ve realm rolleri | Kimlik verisinin ana kaynagi |
| Redis | Gateway IP blacklist ve rate-limit destek verisi | Gecici guvenlik/limit verisi |
| Kafka | Domain olayi ve CQRS projection tasimasi | Kalici is verisinin sahibi degildir |
| PostgreSQL | Command ve operasyonel servislerin kalici yazma tarafi | Islem, bakiye, musteri ve denetim kayitlari |
| MongoDB | CQRS read model | Sorgu icin kopyalanmis gorunum |
| Elasticsearch | Arama ve filtreleme indisi | Mongo read modelin arama odakli ikincil kopyasi |

Gateway JWT'yi Keycloak issuer'i ile dogrular. JWT dogrulandiginda `sub`, e-posta, kullanici adi, ad, soyad ve roller sonraki servislere `X-User-*` header'lari olarak aktarilir. Bu sayede servisler istemcinin gonderebilecegi guvenilmez bir kullanici kimligi yerine Gateway'in JWT'den cikardigi Keycloak kimligini kullanir.

`/api/user-service/v1/auth/**` giris, kayit ve token yenileme icin publictir. Diger kullanici endpointleri JWT ister. Admin rotalari Gateway'de `ADMIN` rolune ayrilmistir.

## Servis Haritasi ve Veri Sahipligi

| Servis | Ana sorumluluk | Yazdigi ana veri | Okudugu veya ilettigi veri |
| --- | --- | --- | --- |
| `user-service` | Kimlik yasam dongusu, Keycloak yonetimi, kullanici dogrulamasi | Keycloak; idempotency kayitlari icin kendi isleme alindi durumu | Customer onboarding gRPC, kullanici olusturma ve kullanici dogrulama eventleri |
| `customer-service` | Musteri domain facade'i | Veri yazmaz | Command ve query servislerine gRPC dagitimi |
| `customer-service-command` | Musteri profilinin write-side sahibi | `banking_customer_command` PostgreSQL | Musteri projection eventi uretir |
| `customer-service-query` | Musteri read-side ve arama | `banking_customer_query` MongoDB ve Elasticsearch indisi | Musteri projection eventini tuketir |
| `money-service` | Operasyonel hesap, IBAN, bakiye ve transaction parasal hareketi | `banking_money` PostgreSQL | Kullanici olusturma, transaction, saga ve sonuc eventleri |
| `money-service-command` | Ayrik hesap CQRS write API | Kendi command PostgreSQL verisi | Para projection eventi uretir |
| `money-service-query` | Ayrik hesap CQRS read API | `banking_money_query` MongoDB ve Elasticsearch indisi | Para projection eventini tuketir |
| `transaction-service` | Islem kaydi, durum makinesi, iptal ve ters kayit istegi | `banking_transactions` PostgreSQL | Transaction eventlerini baslatir ve durumlarini eventlerden gunceller |
| `fraud-service` | Transfer dogrulama asamasinin fraud gecidi | `banking_fraud` PostgreSQL idempotency/event kaydi | Kullanici dogrulama basarisini fraud-checked eventine tasir |
| `admin-service` | Operasyon paneli, denetim istegi ve altyapi sorgu facade'i | Veri yazmaz | Admin command/query ile gRPC ve asenkron denetim kaydi |
| `admin-service-command` | Admin islem gecmisi write-side | `banking_admin_command` PostgreSQL | Admin history projection eventi uretir |
| `admin-service-query` | Admin islem gecmisi read-side | `banking_admin_query` MongoDB ve Elasticsearch indisi | Admin history projection eventini tuketir |

## Iletisim Sozlesmeleri

| Cagiran | Hedef | Kanal | Neden |
| --- | --- | --- | --- |
| Istemci | Gateway | HTTPS/REST | Platforma tek giris |
| Gateway | Her domain servisi | HTTP/REST | Route edilen dis API |
| `user-service` | Keycloak | Keycloak Admin ve token API | Kullanici, rol, login, refresh, logout |
| `user-service` | `customer-service` | gRPC, `9202` | Kayit sirasinda zorunlu musteri onboarding |
| `customer-service` | `customer-service-command` | gRPC, `9200` | Musteri write komutlari |
| `customer-service` | `customer-service-query` | gRPC, `9201` | Musteri sorgulari |
| `transaction-service` | `user-service` | gRPC, `9194` | Authorization token ayrintilarini cozme |
| `admin-service` | `admin-service-command` | gRPC, `9197` | Senkron admin history upsert |
| `admin-service` | `admin-service-query` | gRPC, `9198` | Admin history sorgusu |
| Command servisleri | Query servisleri | Kafka | Eventual-consistent read model senkronizasyonu |
| Transaction, money, user, fraud | Birbirleri | Kafka | Uzun surebilen parasal islem ve saga durumu |

## Ana Akis: Kullanici Kaydi ve Musteri Onboarding

Dis endpoint `POST /api/user-service/v1/auth/register` ile baslar. Bu akis iki farkli kimligi baglar:

- Keycloak kullanici kimligi: Guvenlik, sifre ve JWT icin UUID.
- Customer kimligi: Bankacilik profili, KYC, risk, MFA ve musteri durumu icin UUID.

### Basarili Kayit Sirasi

1. Istemci kayit istegini Gateway uzerinden `user-service`e gonderir.
2. `user-service`, `KeycloakAdminService` ile Keycloak banking realm'inda kullaniciyi `USER` roluyle olusturur. Bu adimdan Keycloak UUID geri doner.
3. `user-service`, bu UUID ve kayit bilgisini `CustomerOnboardingGrpcClient` ile `customer-service`e gonderir.
4. `customer-service`, onboarding istegini `CustomerOnboardingGrpcEndpoint` uzerinden alir ve kendi `CustomerService` facade'ina iletir.
5. Facade, `customer-service-command`e gRPC ile create komutu gonderir.
6. `customer-service-command`, Keycloak UUID ve e-posta icin aktif kayit tekillik kontrolu yapar.
7. Command servisinde Customer modeli olusturulur. Model; Keycloak realm, e-posta ve telefon dogrulamasi, kullanici tipi, durum, dogum bilgisi, ad-soyad, cinsiyet, risk skoru, MFA, tercih dili, ozel musteri, KYC ve uyruk alanlarini tasir.
8. Customer kaydi `banking_customer_command` PostgreSQL veritabanina yazilir. Bu musteri profilinin write-side gercek kaynagidir.
9. Command servisi `banking-microservices.customer.projection-sync.v1` topic'ine `CUSTOMER_CREATED` projection olayini yazar.
10. `customer-service-query` olayi tuketir; MongoDB customer read modelini ve Elasticsearch customer arama indisini gunceller.
11. Command gRPC cevabi tekrar `customer-service` ve `user-service`e doner. Bu anda zorunlu onboarding basarili kabul edilir.
12. `user-service`, `banking-microservices.user.created.v1` topic'ine Keycloak UUID'yi yollar.
13. `money-service` bu olayi tuketir, ayni UUID icin idempotency kontrolu yapar, kullaniciya hesap ve IBAN olusturur ve bunu `banking_money` PostgreSQL veritabanina yazar.
14. `money-service`, `banking-microservices.money.account.created.v1` olayini yollar.
15. `user-service` bu basari olayini alir ve event UUID icin tekrar islemeyi engelleyen kayit mekanizmasini uygular.

Kayit HTTP cevabinin donmesi, musteri command kaydinin basarisina baglidir. Buna karsin customer query modeli ve hesap olusumu Kafka ile ilerledigi icin eventual consistency vardir. Kayit basarili cevabindan hemen sonra `GET /api/customer-service/v1/customers/me` ya da hesap sorgusu kisa sureligine read model bulunamadi cevabi verebilir; projection tamamlandiginda veri gorunur hale gelir.

### Kayit Hatasi ve Geri Alma Sinirlari

| Hata noktasi | Mevcut davranis | Veri sonucu |
| --- | --- | --- |
| Keycloak kullanici olusturulamiyor | Akis hemen durur | Customer ve hesap olusmaz |
| Customer onboarding gRPC veya customer command adimi basarisiz | `user-service` Keycloak kullanicisini silmeyi dener | Kalici musteri kaydi olusmamis ise tutarlidir |
| Keycloak silme islemi de basarisiz | Asil hataya suppressed rollback hatasi eklenir ve loglanir | Manuel operasyon gerektiren yetim Keycloak kimligi kalabilir |
| Customer basarili olduktan sonra `user.created` Kafka gonderimi basarisiz | Genel hata yakalama Keycloak kullanicisini silmeyi dener | Customer command kaydi icin telafi adimi bulunmadigindan Keycloak'suz customer kaydi kalabilir |
| Money hesap olusturma basarisiz | Money tarafinda hata olayi uretilir | Kullanici ve customer kaydi kalir; hesap acilisinin yeniden denetlenmesi gerekir |

Son iki satir, sistemin mevcut kod davranisidir. Bankacilikta bu sinirlar ileride outbox, saga orchestration veya customer soft-delete telafisi ile guclendirilmelidir; bu belge mevcut akisla hedef mimariyi birbirine karistirmamak icin durumu acikca yazar.

## Giris, Token ve Kimlik Bilgisi Akisi

1. Istemci `POST /api/user-service/v1/auth/login` ile e-posta/kullanici bilgisi ve sifre gonderir.
2. `user-service`, `KeycloakUserService` uzerinden Keycloak token endpointine gider.
3. Keycloak access token ve refresh token dondurur.
4. Istemci sonraki isteklerde access token'i Authorization header'inda Gateway'e gonderir.
5. Gateway token imzasini ve issuer bilgisini dogrular, JWT realm rollerini Spring Security rollerine cevirir.
6. Gateway `sub`, e-posta, ad, soyad ve rolleri `X-User-KeycloakUUID`, `X-User-Email`, `X-User-Name`, `X-User-Surname` ve `X-User-Roles` header'larina ekler.
7. Domain servisleri kullanici sahipligi kontrolunu bu Gateway kaynakli header'lar ve gerekli yerde token gRPC cozumu ile yapar.

`refresh` token yeniler, `logout` ise Keycloak oturumunu sonlandirir. Kullanici detaylari ve rol yonetimi `user-service`in admin endpointleriyle Keycloak uzerinde yurutulur.

## Musteri Profili Okuma ve Guncelleme Akisi

### Kullanici kendi profilini goruntuler

1. Istemci `GET /api/customer-service/v1/customers/me` cagirir.
2. Gateway JWT'den Keycloak UUID'yi ekler.
3. `customer-service`, bu UUID ile `customer-service-query`e gRPC sorgusu yapar.
4. Query servisi `banking_customer_query` MongoDB read modelinden aktif customer kaydini bulur.
5. Cevap facade uzerinden istemciye doner.

### Kullanici kendi profilini veya MFA tercihini gunceller

1. Istemci profil icin `PUT /me/profile`, MFA icin `PATCH /me/mfa` cagirir.
2. `customer-service`, requestteki customer UUID ile Gateway'den gelen Keycloak UUID'nin ayni musteriye ait oldugunu sorgulayarak sahiplik kontrolu yapar.
3. Basarili istek command gRPC ile `customer-service-command`e iletilir.
4. Command servisi ilgili helper sinifini kullanarak sadece hedef alanlari degistirir, is kurallarini dogrular ve PostgreSQL write modelini kaydeder.
5. `PROFILE_UPDATED`, `MFA_UPDATED` veya ilgili operation type ile customer projection eventi uretilir.
6. Query servisi MongoDB ve Elasticsearch'i gunceller.

Admin musteri endpointleri ayni command/query zincirini kullanir; fark, Gateway'in `ADMIN` rolu zorunlulugu ve adminin durum, KYC, risk, contact verification, special customer ve soft-delete operasyonlarina da erisebilmesidir.

## Hesap ve Bakiye Akisi

### Operasyonel Hesap Servisi

`money-service`, kullanicinin operasyonel banka hesabinin sahibidir. Hesap olusumu normalde kullanici kaydindaki `user.created` olayi ile tetiklenir. Servisin kendi HTTP endpointleri; bakiye ve IBAN goruntuleme, kullanici UUID ile hesap bulma, para yatirma, para cekme ve admin hesap yonetimi icindir.

Kullanici kendi bakiyesini `GET /api/money-service/v1/accounts/balance-info` ile ister. Gateway'in ekledigi Keycloak UUID ile `money-service`, `banking_money` veritabanindan kendi hesabini bulur.

### Ayrik Money CQRS Akisi

`money-service-command` ve `money-service-query`, operasyonel `money-service`ten ayri bir CQRS hesap API'si sunar. Bu akis su an normal kullanici kaydi ve transaction parasal hareket zincirinin zorunlu parcasi degildir.

1. Command API create, deposit, withdraw, block-money veya transfer istegini alir.
2. `money-service-command` kendi write modelini degistirir.
3. Her basarili durum degisiminden sonra `banking-microservices.money.projection-sync.v1` topic'ine olay yazar.
4. `money-service-query` bu olayi tuketir.
5. Query servisi MongoDB'de ana read modeli, Elasticsearch'te arama indisini gunceller.
6. Query API ID, user ID, IBAN veya keyword ile bu read modelden cevap verir.

Bu ayrimin sonucu: Yeni hesap ozelligi yazilirken once hangi bounded contextin sahibi oldugu belirlenmelidir. Gercek parasal transaction islemine baglanacak veri `money-service` akisini, raporlama veya ayrik CQRS hesap API'si ise `money-service-command/query` akisini takip etmelidir.

## Transaction Akisi: Ortak Baslangic

Dis endpoint `POST /api/transaction-service/v1/transactions/create`tir.

1. Gateway JWT claim'lerini header'lara ekler.
2. `transaction-service`, Authorization token ayrintilarini `user-service`e gRPC ile cozdurur.
3. Servis her islem icin benzersiz bir event UUID uretir.
4. `TransactionEntity`, baslangicta `CREATED` durumu ile `banking_transactions` PostgreSQL veritabanina yazilir. Bu kayit islem gecmisi ve durum takibinin ana kaynagidir.
5. Transaction servisi `banking-microservices.transaction.created.v1` topic'ine event gonderir.
6. Sonraki davranis transaction tipine gore `money-service` tarafinda ayrilir.

Transaction kaydi parasal hareketten once olusturulur. Bu, istemciye hemen kabul cevabi donmeyi ve daha sonra eventlerle `CREATED`, `BLOCK_MONEY`, `VALIDATION_PENDING`, `FRAUD_REVIEW`, `COMPLETED` veya hata durumlarina gecmeyi saglar.

### Para Yatirma

1. Transaction created olayi `money-service` tarafinda alinir.
2. Transaction tipi `DEPOSIT` ise hedef IBAN veya kullanici UUID ile hesap bulunur.
3. Bakiye `money-service` veritabaninda artirilir.
4. Event `COMPLETED` durumuna getirilir ve `banking-microservices.transaction.completed.v1` topic'ine gonderilir.
5. `transaction-service` completion eventini alir ve kendi TransactionEntity durumunu gunceller.

### Para Cekme

1. Transaction created olayi `money-service` tarafinda alinir.
2. Transaction tipi `WITHDRAW` ise hedef hesap bulunur ve bakiye kontrol edilir.
3. Bakiye `money-service` veritabaninda azaltillir.
4. Basari durumunda `COMPLETED` eventi gonderilir.
5. `transaction-service` kendi islem kaydini completed yapar.

Yetersiz bakiye, IBAN bulunamamasi veya parasal islem hatasinda `money-service` hata durumunu `banking-microservices.transaction.failed.v1` topic'ine yollar. `transaction-service` bu olayi dinler ve kaydi hata durumuna gecirir.

### Havale veya EFT Transferi

Transfer, para yatirma ve cekmeden farkli olarak iki hesap ve ek dogrulama gerektirdigi icin asamali ilerler.

1. `transaction-service`, `CREATED` transaction kaydini ve created eventini olusturur.
2. `money-service`, transfer eventini alir ve gonderen IBAN'i, alici hesabi ve uygun bakiyeyi dogrular.
3. Gonderenin kullanilabilir bakiyesi azaltilir, ayni miktar blocked balance'a tasinir. Bu adim paranin tekrar harcanmasini engeller.
4. `money-service`, `banking-microservices.transaction.money-blocked.v1` olayini yollar. `transaction-service` kendi kaydini `BLOCK_MONEY` durumuna getirir.
5. Aynı money adimi, `banking-microservices.transaction.user-validation.request.v1` olayini da yollar.
6. `user-service`, gondereni Keycloak verisinden bulur. Transfer ise aliciyi ad-soyad ile bulur, alici UUID ve e-postasini olaya ekler.
7. Kullanici dogrulamasi basariliysa `banking-microservices.transaction.user-validation.success.v1` olayi gonderilir. Basarisizsa `transaction.failed.v1` gonderilir.
8. `transaction-service` basarili dogrulama olayini alip durumunu `VALIDATION_PENDING` olarak gunceller.
9. `fraud-service`, user-validation success olayini alir, event UUID ile tekrar islemeyi engeller, olayi `FRAUD_REVIEW` durumuna getirir ve `banking-microservices.transaction.fraud.checked.v1` topic'ine iletir.
10. `money-service`, fraud-checked transfer olayini alir. Blokeli tutardan gonderen icin cekim yapar, alici hesaba yatirim yapar.
11. Her iki parasal adim basariliysa `COMPLETED` sonucu `transaction.completed.v1` ile yollanir.
12. `transaction-service` completion olayini alip TransactionEntity durumunu `COMPLETED` yapar.

Fraud servisinin mevcut kodu bir fraud kural motoru ile red karari vermemektedir. Mevcut davranis, event'i idempotent sekilde kaydederek `FRAUD_REVIEW` durumuyla bir sonraki asamaya iletmektir. Gercek fraud kural motoru eklendiginde bu nokta red, ek dogrulama veya onay kararinin sahibi olmalidir.

## Transaction Iptal ve Saga Ile Geri Alma

Kullanici `POST /api/transaction-service/v1/transactions/cancel` ile kendi gonderdigi islemi iptal etmek isteyebilir. Transaction servisi event UUID'yi bulur ve kullanicinin gonderen kimliginin kayittaki gonderenle esit oldugunu kontrol eder.

- Islem henuz tamamlanmadiysa kayit `CANCELLED` olur. Bu sadece transaction kaydinin durumudur; event akisi icin ek telafi event'i uretilmez.
- Islem `COMPLETED` ise servis bir saga kaydi olusturur, saga olayi `banking-microservices.transaction.saga.created.v1` topic'ine yollar ve transaction kaydini `REVERSED` yapar.
- Admin `POST /api/transaction-service/v1/admin/transactions/reverse` ile ayni geri alma yolunu kullanabilir. Admin kullanici sahipligi kontrolunden muaftir.

Saga akisinda `money-service` saga created olayini alir, once kendi saga kaydini `PROCESS` durumuyla saklar ve transaction tipine gore telafi yapar:

| Orijinal islem | Money-service telafisi |
| --- | --- |
| Transfer | Alicidan geri cekme, gonderene geri yatirma |
| Deposit | Hedef hesaptan geri cekme |
| Withdraw | Hedef hesaba geri yatirma |

Telafi basariliysa saga `COMPLETED` olur ve `banking-microservices.transaction.saga.money.completed.v1` olayiyla transaction servisine bildirilir. Telafi basarisizsa saga `ERROR` olur ve `transaction.saga.money.failed.v1` olayiyla raporlanir. `transaction-service` bu eventlerle kendi SagaEvents kaydinin durumunu gunceller.

## Admin ve Denetim Gecmisi Akisi

`admin-service`, admin panelinin facade katmanidir. Kubernetes, Kafka, CQRS, backup, database ve log sorgularini baslatir; bu operasyonlarin denetim gecmisini ayri bir CQRS zincirinde tutar.

### Senkron Admin Islemi

1. Admin istegi Gateway'de `ADMIN` rolunden gecer.
2. `admin-service`, request ID uretir ve admin baglamini cikarir.
3. Islem sonucu ile birlikte `admin-service-command`e gRPC ile history upsert istegi gonderilir.
4. Command servisinde `banking_admin_command` PostgreSQL kaydi eklenir veya ayni request ID ile guncellenir.
5. Command servisi `banking-microservices.admin.history.projection-sync.v1` olayini yollar.
6. `admin-service-query`, MongoDB ve Elasticsearch read modelini gunceller.

### Asenkron Admin Islemi

1. Uzun surebilen istek ilk olarak `PENDING` durumuyla `banking-microservices.admin.history.command.v1` topic'ine yazilir.
2. `admin-service-command` topic'i tuketir ve history kaydini upsert eder.
3. Asenkron is tamamlandiginda admin facade sonucu ikinci bir command olayi olarak yollar.
4. Command ve query taraflari ayni request ID uzerinden en guncel denetim gorunumunu olusturur.
5. Admin paneli history verisini `admin-service-query`den gRPC ile okur.

## CQRS Projection Kurallari

Customer, money ve admin query servisleri ayni temel prensiple calisir:

1. Write-side PostgreSQL'e basarili olarak yazilir.
2. Bir operation type ve aggregate kimligi tasiyan projection eventi Kafka'ya basilir.
3. Query-side event zamanini kontrol eder.
4. Daha yeni bir projection zaten varsa eski event yok sayilir.
5. MongoDB ana read model olarak guncellenir.
6. Elasticsearch arama ve filtreleme icin ikinci kopya olarak guncellenir.

Bu tasarim, command ve query verilerinin anlik olarak birebir ayni olmayabilecegi anlamina gelir. Yeni endpointler yazilirken write sonrasi hemen read cagrisi yapilacaksa eventual consistency ve retry/uygun hata cevabi hesaba katilmalidir.

## Kafka Topic Haritasi

| Topic | Uretici | Tuketici | Anlam |
| --- | --- | --- | --- |
| `banking-microservices.user.created.v1` | `user-service` | `money-service` | Yeni kullanici icin hesap acma |
| `banking-microservices.money.account.created.v1` | `money-service` | `user-service` | Hesap acilis basarisi |
| `banking-microservices.money.account.create-failed.v1` | `money-service` | Operasyonel takip | Hesap acilis hatasi |
| `banking-microservices.customer.projection-sync.v1` | `customer-service-command` | `customer-service-query` | Musteri write-side -> read-side |
| `banking-microservices.money.projection-sync.v1` | `money-service-command` | `money-service-query` | Ayrik money CQRS write-side -> read-side |
| `banking-microservices.admin.history.command.v1` | `admin-service` | `admin-service-command` | Asenkron admin history komutu |
| `banking-microservices.admin.history.projection-sync.v1` | `admin-service-command` | `admin-service-query` | Admin history write-side -> read-side |
| `banking-microservices.transaction.created.v1` | `transaction-service` | `money-service` | Yeni deposit, withdraw veya transfer istegi |
| `banking-microservices.transaction.money-blocked.v1` | `money-service` | `transaction-service` | Transfer tutari bloke edildi |
| `banking-microservices.transaction.user-validation.request.v1` | `money-service` | `user-service` | Transfer taraflarini dogrulama istegi |
| `banking-microservices.transaction.user-validation.success.v1` | `user-service` | `transaction-service`, `fraud-service` | Kullanici dogrulama basarisi |
| `banking-microservices.transaction.fraud.checked.v1` | `fraud-service` | `money-service` | Fraud gecidinden gecen transfer |
| `banking-microservices.transaction.completed.v1` | `money-service` | `transaction-service` | Parasal hareket tamamlandi |
| `banking-microservices.transaction.failed.v1` | `user-service` veya `money-service` | `transaction-service` | Akis hatasi ve durum guncellemesi |
| `banking-microservices.transaction.saga.created.v1` | `transaction-service` | `money-service` | Tamamlanmis islemin telafi istegi |
| `banking-microservices.transaction.saga.money.completed.v1` | `money-service` | `transaction-service` | Saga telafisi basarili |
| `banking-microservices.transaction.saga.money.failed.v1` | `money-service` | `transaction-service` | Saga telafisi basarisiz |

## Idempotency, Sira ve Gozlemlenebilirlik

- `user-service`, `money-service` ve `fraud-service` tekrar gelen eventleri event UUID ile engelleyen kayit mekanizmalarina sahiptir.
- Transaction durum guncellemeleri ayni event ve ayni durum icin tekrar edilmez.
- Customer ve money projection servisleri eski zamanli eventlerin yeni read modeli ezmesini engeller.
- Customer modelinde optimistic locking icin version alanı, audit alanlari ve soft delete vardir.
- Servisler kritik gecislerde `log.info`, hata gecislerinde `log.warn` veya `log.error` ile event UUID, customer UUID ya da Keycloak UUID bilgisi loglar.
- Transaction event UUID, saga Kafka event UUID ve admin request ID; servisler arasi iz surmede kullanilacak ana korelasyon alanlaridir.

## Yeni Gelistirmeler Icin Karar Kurali

Yeni bir istek geldiginde once su sirayla karar verilmelidir:

1. Veri hangi domainin gercek sahibi: Keycloak, Customer, Money, Transaction veya Admin?
2. Islem sonucunun HTTP cevabindan once kesinlesmesi gerekiyor mu? Gerekiyorsa mevcut facade ve gRPC akisi kullanilir.
3. Sorgu performansi, arama veya raporlama mi gerekiyor? Gerekiyorsa mevcut command -> Kafka projection -> MongoDB/Elasticsearch akisi genisletilir.
4. Birden fazla serviste parasal veya kalici durum degisikligi var mi? Varsa event UUID, idempotency ve saga/telafi adimi tasarlanir.
5. Var olan bir akis ayni davranisi zaten yapiyor mu? Varsa yeni paralel akis olusturmak yerine o akis genisletilir.

Bu kurallar, yeni mikroservis veya model eklenirken repository, exception, helper, validation, log, gRPC, Kafka event, projection ve test ihtiyacinin domain akisindan sistematik olarak cikarilmasini saglar.

## Incelenen Baslica Kaynaklar

- `gateway/src/main/resources/application.yaml` ve Gateway security/filter siniflari
- `user-service` auth, Keycloak, Kafka ve customer onboarding siniflari
- `customer-service`, `customer-service-command`, `customer-service-query` controller, gRPC, command ve projection siniflari
- `money-service`, `money-service-command`, `money-service-query` Kafka, command ve query siniflari
- `transaction-service` controller, transaction service, listener ve saga kayit siniflari
- `fraud-service` Kafka listener ve event repository siniflari
- `admin-service`, `admin-service-command`, `admin-service-query` history dispatch, command ve projection siniflari
- `k8s/01-Base.yaml` ve `k8s/02-Apps.yaml`
