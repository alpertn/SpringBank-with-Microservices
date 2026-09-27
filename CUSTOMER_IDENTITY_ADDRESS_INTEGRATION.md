# Customer Kimlik ve Adres Entegrasyonu

## 1. Sonuç

Mevcut kullanıcı oluşturma akışı korunmuştur. Yeni bir onboarding akışı üretilmemiştir. Kullanıcı önce Keycloak'ta oluşturulur; ardından mevcut gRPC hattı üzerinden customer-service çağrılır. Customer kaydı veya adres ayrıştırma başarısız olursa mevcut compensation adımı Keycloak kullanıcısını siler.

Adres ayrıştırma, `customer-service-command` ile aynı Kubernetes Pod'u içinde fakat ayrı container olarak çalışan libpostal REST sidecar'ına yaptırılır. PostgreSQL tek yazma kaynağıdır. Başarılı kayıt Kafka projection olayıyla MongoDB ve Elasticsearch modellerine aktarılır.

## 2. Uçtan Uca Veri Akışı

1. İstemci `user-service /auth/register` endpoint'ine kimlik, iletişim ve ham adres bilgilerini yollar.
2. `user-service`, kullanıcıyı Keycloak'ta oluşturur ve Keycloak UUID'sini alır.
3. `user-service`, mevcut customer onboarding gRPC sözleşmesiyle `customer-service` facade'ına gider.
4. `customer-service`, create isteğini customer command gRPC sözleşmesine dönüştürür.
5. `customer-service-command`, e-posta ve Keycloak UUID tekrar kontrolünü yapar.
6. T.C. kimlik numarasının resmi checksum kuralları doğrulanır. Açık değer saklanmaz; HMAC-SHA-256 özeti ve maskeli görünüm üretilir.
7. Command servisi `http://127.0.0.1:8080/v1/addresses/parse` adresindeki sidecar'a senkron REST isteği yollar.
8. Sidecar libpostal modelini kullanarak ham adresi parçalar ve standart REST cevabı döndürür.
9. Command servisi ham adresi ve ayrıştırılmış alanları PostgreSQL `customers` tablosuna tek müşteri kaydının gömülü adres alanları olarak yazar.
10. `CUSTOMER_CREATED` projection olayı Kafka'ya gönderilir. Olayda açık T.C. kimlik numarası bulunmaz.
11. `customer-service-query` olayı tüketir; MongoDB müşteri görünümünü ve Elasticsearch arama dokümanını günceller.
12. Başarı cevabı aynı gRPC zinciri üzerinden `user-service`e döner. Herhangi bir hata durumunda user-service Keycloak kaydını siler.

## 3. Kayıt İsteği Örneği

```json
{
  "email": "ayse.yilmaz@example.com",
  "name": "Ayşe",
  "surname": "Yılmaz",
  "password": "StrongPassword-Example",
  "phoneNumber": "+905551112233",
  "birthdate": "1992-04-18T00:00:00",
  "nationalId": "10000000146",
  "nationalityCode": "TR",
  "maritalStatus": "MARRIED",
  "rawAddress": "Caferağa Mahallesi Moda Caddesi No: 10 Kat: 2 Daire: 5 Kadıköy İstanbul 34710 Türkiye",
  "addressCountryCode": "TR"
}
```

`10000000146` yalnızca format/checksum örneğidir; gerçek kişiye ait veri kullanılmamalıdır.

## 4. Libpostal REST Sözleşmesi

Command servisinin sidecar'a gönderdiği istek:

```json
{
  "address": "Caferağa Mahallesi Moda Caddesi No: 10 Kat: 2 Daire: 5 Kadıköy İstanbul 34710 Türkiye",
  "countryCode": "TR"
}
```

Sidecar'ın döndürdüğü normalize cevap:

```json
{
  "country": "türkiye",
  "countryCode": "TR",
  "province": "istanbul",
  "district": "kadıköy",
  "neighborhood": "Caferağa",
  "road": "moda caddesi",
  "building": null,
  "buildingNumber": "10",
  "entrance": null,
  "floor": "2",
  "unit": "5",
  "postalCode": "34710",
  "parser": "libpostal",
  "parserVersion": "25099c506612b34b23b1bfe286ca6321fcf06f35",
  "components": [
    {"label": "suburb", "value": "caferağa mahallesi"},
    {"label": "road", "value": "moda caddesi"},
    {"label": "house_number", "value": "10"}
  ]
}
```

Sidecar, libpostal sonucundan sonra Türkçe `Mahallesi`, `No`, `Kat` ve `Daire` belirteçlerini deterministik olarak düzeltir; ham libpostal bileşenleri denetim için `components` içinde korunur. Libpostal olasılık/güven skoru üretmediği için cevapta uydurma bir confidence alanı yoktur. Ayrıştırma, posta adresinin gerçek olduğunu da kanıtlamaz. Adres doğrulama veya teslim edilebilirlik için ileride ayrı bir resmi adres/geocoding sağlayıcısı gerekir.

## 5. Customer Modeli

### Kimlik ve iletişim

- `keycloakId`: Keycloak kullanıcısına teknik bağlantı.
- `nationalIdHash`: HMAC-SHA-256; benzersizlik ve eşleştirme için kullanılır.
- `nationalIdMasked`: API ve operasyon ekranlarında güvenli gösterim.
- `email`, `emailVerified`, `phoneNumber`, `phoneVerified`: İletişim ve doğrulama durumu.
- `nationalityCode`: Vatandaşlık kodu.
- `maritalStatus`: Medeni durum.

### Durum, KYC ve risk

- `status`: Müşteri profilinin yaşam döngüsü.
- `accountStatus`: Bankacılık hesabı/ilişkisinin kullanım durumu; profil durumundan ayrıdır.
- `kycStatus`: `PENDING`, `APPROVED`, `REJECTED`.
- `riskScore`: 0-100 ayrıntılı skor.
- `riskClass`: `RISK_1` ile `RISK_7` arasında politika sınıfı.
- `segment`: Ticari segment; `STANDARD`, `MASS_AFFLUENT`, `PREMIUM`, `VIP`, `PRIVATE_BANKING`, `SME`, `CORPORATE`.
- `segmentScore`: 1-10 aralığında segmentleme skoru.
- `politicalExposureStatus`: Siyasi nüfuz bilgisini ticari segmentten ayırır.
- `employmentCategory`: Devlet çalışanı, banka çalışanı veya yönetici gibi çalışma kategorisini segmentten ayırır.

Siyasi kişi, devlet çalışanı ve banka çalışanı aynı `segment` enum'una konulmamıştır. Bunlar farklı karar amaçlarına sahiptir: segment ürün/fiyatlama içindir; PEP AML/KYC kontrolü içindir; çalışma kategorisi çıkar çatışması ve uygunluk kontrolleri içindir.

### Adres

- `AddressType`: `RESIDENTIAL`, `WORK`, `REGISTERED`, `CORRESPONDENCE`.
- Adres parçaları enum değildir; gerçek değer taşıyan `province`, `district`, `neighborhood`, `road`, `buildingNumber`, `entrance`, `floor`, `unit`, `postalCode` alanlarıdır.
- `rawAddress`: Kullanıcının gönderdiği orijinal metin; yeniden ayrıştırma ve denetim için korunur.
- `parseStatus`, `parser`, `parserVersion`: Sonucun hangi motorla üretildiğini izler.

### Audit

`Customer`, mevcut `BaseEntity`yi genişletmeye devam eder. PostgreSQL'de `createdAt`, `updatedAt`, `createdBy`, `updatedBy`, optimistic lock `version` ve soft delete `isDeleted` alanları korunmuştur.

## 6. PostgreSQL Değişiklik Yönetimi

Hibernate `ddl-auto=update` kaldırılmış ve `validate` yapılmıştır. Şema değişiklikleri artık Flyway ile sürümlüdür:

- `V1__create_customers.sql`: Yeni kurulumda temel customer tablosunu oluşturur.
- `V2__extend_customer_identity_and_address.sql`: Kimlik, segment, risk sınıfı ve ayrıştırılmış adres kolonlarını ekler.
- Eski satırlar migration sırasında `legacy:<customer-id>` kimlik özetiyle işaretlenir; bunlar gerçek TCKN sayılmaz ve kontrollü veri tamamlama sürecine alınmalıdır.
- `segment_score` ve `risk_score` PostgreSQL CHECK constraint ile de korunur.

## 7. Hata Davranışı

- Geçersiz TCKN checksum: kayıt reddedilir ve Keycloak compensation çalışır.
- Libpostal timeout/5xx/boş cevap: `ADDRESS_PARSER_UNAVAILABLE`; customer yazılmaz ve Keycloak compensation çalışır.
- Aynı e-posta, Keycloak UUID veya TCKN özeti: unique constraint ve servis kontrolüyle conflict.
- Kafka projection başarısızlığı: mevcut akış exception üretir. Tam kurumsal güvenilirlik için sonraki adım transactional outbox olmalıdır; veritabanı kaydı ile Kafka yayını aynı atomik sınırda değildir.

## 8. Docker ve Kubernetes

- Image: `springbankwithmicroservices/libpostal-service:1.0.0`.
- Libpostal ve resmi Python binding commit'leri Dockerfile içinde sabitlenmiştir.
- Sidecar dışarı açılmaz; yalnızca aynı Pod içindeki command servisi erişir.
- Readiness ve liveness endpoint'leri vardır.
- Model dosyaları birkaç GB olabilir ve parser yüksek bellek kullanabilir. Başlangıç süresi ve node disk kapasitesi izlenmelidir.
- Her command replica kendi sidecar'ını taşır. Bu güçlü izolasyon sağlar fakat maliyeti yüksektir; çok yüksek replica sayısında ayrı, yatay ölçeklenen internal address-parser deployment daha ekonomik olabilir.
- `customer-service-secrets` içindeki anahtar yalnızca geliştirme placeholder'ıdır. Üretimde Vault/KMS/External Secrets ile rastgele ve rotasyonlu anahtar verilmelidir. Anahtar kontrolsüz değişirse aynı TCKN farklı hash üretir; rotasyon çift-anahtarlı geçiş planıyla yapılmalıdır.

## 9. Çalıştırma ve Sağlık Kontrolü

Ana image build menüsüne libpostal image adımı eklenmiştir. Kubernetes manifesti command container ile sidecar'ı aynı Pod'a yerleştirir. Sidecar hazır olmadan command Pod'u hazır kabul edilmez.

Sağlık uçları:

- `GET /health/live`: süreç çalışıyor mu?
- `GET /health/ready`: libpostal yüklenmiş ve istek kabul edebilir mi?
- `POST /v1/addresses/parse`: ayrıştırma.

## 10. Bilinçli Sınırlar ve Sonraki Kurumsal Adımlar

- Adres ayrıştırma, adres doğrulama değildir. Türkiye için UAVT/MAKS veya yetkili veri sağlayıcısıyla adres kodu doğrulaması ayrıca tasarlanmalıdır.
- PEP statüsü kullanıcı beyanıyla güvenilir kabul edilmemeli; yaptırım/PEP tarama servisi tarafından üretilmelidir.
- `riskClass` ve `segment` kullanıcı create isteğinden serbestçe alınmamalı; mevcut uygulamada onboarding güvenli varsayılanlarla başlatır, daha sonra yetkili risk/CRM akışı günceller.
- Tek bir embedded adres şu anki onboarding ihtiyacını karşılar. Birden fazla tarihçeli adres için sonraki sürümde ayrı `customer_addresses` aggregate/tablosu, geçerlilik tarihleri ve `primary` kuralı gerekir.
- Projection teslim garantisi için transactional outbox + consumer inbox eklenmelidir.
