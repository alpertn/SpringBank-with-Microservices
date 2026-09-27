# Codex Project Memory

Bu dosya, SpringBank-with-Microservices projesinde bundan sonra Codex'in calisma standardini tanimlar.
Kullanici yeni bir mikroservis, model, endpoint, akis veya refactor istediginde once bu projedeki mevcut kurumsal yapiya bakilmali ve uretilen kod bu mimariye uygun olmalidir.

## Projenin Genel Kimligi

Bu proje real-life banking amacina yakin, mikroservis tabanli, event-driven ve kurumsal seviyeye tasinmak istenen bir Spring Boot bankacilik platformudur.
Kod yazarken proje basit demo gibi ele alinmamalidir.

Ana mimari karakteristikler:

- Java 21 ve Spring Boot tabanli mikroservisler.
- Spring Gateway uzerinden tek giris noktasi.
- Keycloak JWT ile authentication ve role based authorization.
- Kafka ile asenkron event-driven akislar.
- gRPC ile servisler arasi senkron ve hizli internal haberlesme.
- PostgreSQL ile write-side/source-of-truth veri.
- MongoDB ve Elasticsearch ile CQRS read-side projection.
- Redis ile cache, blacklist veya idempotency destekleri.
- Kubernetes, Docker image, HPA, service discovery ve operasyonel deployment dusuncesi.
- Global exception handling, loglama, audit history ve admin operasyon paneli yaklasimi.

## Varsayilan Calisma Davranisi

Kullanici bir is istediginde Codex once ilgili mevcut mikroservisleri incelemeli:

- Istenen isin projede zaten bir akisi var mi?
- Varsa bu akis hangi controller, service, Kafka topic, listener, gRPC client/endpoint, repository ve exception zincirinden geciyor?
- Var olan akis genisletilebilir mi, yoksa gercekten yeni bir akis mi gerekiyor?
- Benzer entity/model siniflari nasil yazilmis?
- Repository pattern nasil kullanilmis?
- DTO/record/class tercihi hangi serviste nasil?
- Exception classlari nasil adlandirilmis?
- GlobalExceptionHandler hangi response formatini donuyor?
- Controller path naming standardi nasil?
- Service katmaninda log.info/log.warn/log.error nasil konumlandirilmis?
- Kafka topic, producer, listener ve event DTO'lari nasil tasarlanmis?
- gRPC proto ve endpoint/client ayrimi nasil yapilmis?
- Testler mevcut pattern'e gore nasil yazilmis?

Bu inceleme yapilmadan yeni yapi uretilmemeli.

## Mevcut Akis Onceligi

Codex hicbir zaman var olan bir akis varken ayni isi yapan yeni ve paralel bir akis olusturmamalidir.
Once projedeki mevcut akis bulunmali, anlasilmali ve gerekiyorsa o akisa entegre olunmalidir.

Sistemdeki mevcut servis sorumluluklari, veri sahipligi, gRPC baglantilari, Kafka topicleri, kullanici kaydi, customer onboarding, hesap acilisi, transaction, fraud, saga, CQRS ve admin history akislarinin ayrintili referansi `DATA_FLOW_ARCHITECTURE.md` dosyasidir. Ayni akislari tasima mekanizmasi, topic, port ve veri deposuyla gorsel olarak gosteren Draw.io referansi `readme/current-data-flow-architecture.drawio` dosyasidir. High-scale hedef, mevcut akislarin eksikleri ve endpointten veritabanina kadar somut hop-by-hop karsilastirma `HIGH_SCALE_BANKING_REFERENCE_ARCHITECTURE.md` belgesinin 23. bolumundedir. Yeni bir domain degisikliginden once ilgili bolum bu uc kaynaktan da kontrol edilmelidir.

Her veri akisi analizinde su zincir acikca yazilmalidir:

- Istegi baslatan kanal ve endpoint.
- Gateway route, authentication ve eklenen identity context.
- Istegi alan controller ve business service.
- Cagrilan mikroservis ve kullanilan REST/gRPC/Kafka transportu.
- gRPC portu veya Kafka topic'inin tam adi.
- Yazilan source-of-truth veritabani ve uretilen read projection.
- Istemciye donen senkron cevap ve arkada devam eden asenkron adimlar.
- Hata, retry, idempotency, rollback/compensation ve reconciliation yolu.

Bir akis yalnizca kutularla veya servis isimleriyle anlatilmis sayilmaz. Hangi
verinin hangi protokolle nereden nereye gittigi ve bir sonraki adimi hangi
event/statusun tetikledigi belirtilmelidir.

Zorunlu akis arastirmasi:

- Kullanici create/register/onboarding isterse once mevcut `create user` akisi incelenmelidir.
- Kullanici transaction/transfer/deposit/withdraw isterse once mevcut transaction, money, fraud ve saga akislarina bakilmalidir.
- Kullanici query/read/search isterse once mevcut CQRS command-query projection yapisi incelenmelidir.
- Kullanici admin/audit/log/monitor isterse once admin-service, admin-service-command ve admin-service-query akislarina bakilmalidir.
- Kullanici security/auth/JWT isterse once gateway, Keycloak ve user-service gRPC token decode akisi incelenmelidir.

Akis incelemesinde bakilacak yerler:

- Controller endpointleri.
- Service metodlari.
- Kafka topic adlari ve event DTO'lari.
- Kafka listener ve producer siniflari.
- gRPC proto, client ve endpoint siniflari.
- Repository ve database ownership.
- Exception ve rollback/compensation davranisi.
- Testlerde akisin nasil temsil edildigi.

Yeni akis sadece su durumda tasarlanabilir:

- Projede benzer bir akis yoksa.
- Mevcut akis yeni ihtiyaci tasiyamayacak kadar farkli bir bounded context'e aitse.
- Mevcut akisa eklemek public API'yi, domain sorumlulugunu veya veri tutarliligini bozacaksa.
- Kullanici acikca yeni ve ayri bir akis istediyse.

Var olan akis varsa varsayilan davranis:

- Ayni event zinciri genisletilir.
- Ayni correlation id/eventUUID/requestId tasinir.
- Ayni hata ve rollback yaklasimi korunur.
- Ayni loglama ve status gecis mantigi izlenir.
- Yeni servis gerekiyorsa mevcut akisa baglanir, akisi bastan kurmaz.
- Mevcut endpoint, topic, proto veya state machine genisletilebiliyorsa paralel
  controller, topic ya da ikinci source-of-truth olusturulmaz.
- Dokumandaki akis ile kod celisirse dokuman dogru varsayilmaz; controller,
  service, config, proto ve listener kodu yeniden okunur ve dokuman guncellenir.

## Mikroservis Olusturma Standardi

Kullanici "bu mikroservisi olustur, modelleri bunlar" dediginde ilk adimda su temel katmanlar uretilmelidir:

- Application main class.
- Model/entity siniflari.
- DTO veya request/response modelleri.
- Repository interface'leri.
- Domain exception classlari.
- GlobalExceptionHandler.
- application.yaml iskeleti.
- Dockerfile.
- Gerekiyorsa proto dosyalari.
- Gerekiyorsa Kafka producer/listener temel yapisi.
- Temel unit test iskeleti.

Bu temel yapi kurulduktan sonra Codex kullaniciya hangi servis ve controller operasyonlarinin eklenebilecegini sormalidir.
Soru belirsiz olmamali; somut oneriler halinde gelmelidir.

Ornek:

"Bu modelden su servis metodlari mantikli gorunuyor: create, update, getById, search, delete, statusChange. Controller tarafinda public user endpointleri ve admin endpointleri ayri yazilabilir. Hangilerini ekleyeyim?"

Kullanici "bunlari ekle, bunlari ekleme" dedikten sonra Codex uygulamayi tamamlamalidir.

## Kod Uretirken Beklenen Akil Yurutme

Codex yalnizca soylenen classlari mekanik olarak uretmemelidir.
Projenin banking ve kurumsal hedefini dusunerek eksik kalabilecek ayrintilari kendisi tamamlamalidir:

- Validation anotasyonlari ve domain guard'lari.
- Idempotency ihtiyaci.
- Transaction boundary ihtiyaci.
- Kafka duplicate event korumasi.
- Error topic veya failure event ihtiyaci.
- Audit veya admin history ihtiyaci.
- Role based endpoint ayrimi.
- DTO ile entity ayrimi.
- Sensitive data'nin loglanmamasi.
- Clear status enum tasarimi.
- Para alanlarinda BigDecimal kullanimi.
- LocalDateTime serialization uyumu.
- Meaningful log.info, log.warn, log.error mesajlari.
- Exception mesajlarinin operasyonel debug icin yeterli olmasi.
- Test edilebilir constructor injection yapisi.
- Unit test ve kritik service testleri.

## Mimari Karar Standartlari

Yeni feature eklerken su kararlar korunmalidir:

- Public giris mumkunse gateway arkasindan olmali.
- User-facing REST endpointler controller katmaninda kalmali.
- Business logic service katmaninda olmali.
- Database erisimi repository disina tasirilmamali.
- Servisler arasi command/event akislari Kafka ile tasarlanabilir.
- Internal hizli sorgu veya token decode gibi senaryolarda gRPC tercih edilebilir.
- Write ve read sorumluluklari buyuyen modullerde CQRS dusunulmelidir.
- Query tarafinda MongoDB ana read model, Elasticsearch arama/index katmani olarak konumlanmalidir.
- Kafka topic adlari version'li ve domain odakli olmali: `banking-microservices.<domain>.<event>.v1`.
- Kritik islemlerde eventUUID/requestId gibi correlation id bulunmalidir.

## Banking Domain Dikkatleri

Para ve hesap islemleri yazilirken demo kolayligi yerine banka mantigi dusunulmelidir:

- Para miktarlari `BigDecimal` olmali.
- Negatif veya sifir tutar reddedilmeli.
- Ayni hesaba transfer engellenmeli.
- Sender/receiver IBAN varligi kontrol edilmeli.
- Yetersiz bakiye net exception ile donmeli.
- Transferlerde gerekirse once para bloke edilmeli, sonra fraud/user validation sonrasi execute edilmeli.
- Completed islem geri alinacaksa saga/compensation dusunulmeli.
- Transaction status'leri acik ve izlenebilir olmali.
- Idempotency olmadan Kafka listener kritik para hareketi yapmamali.

## Guvenlik ve Operasyon Standartlari

Kurumsal seviye hedef icin su noktalar hep hatirlanmali:

- Secret, password ve client secret kodda veya manifestte plain text olmamali; yeni kodda bu aliskanlik devam ettirilmemeli.
- Admin endpointleri role kontrolu altinda olmali.
- Kullanici sadece kendi kaynagini gorebilmeli veya degistirebilmeli.
- Sensitive bilgiler loglanmamali.
- Gateway filtreleri gercek IP, JWT claim ve blacklist gibi cross-cutting konulari merkezi ele almali.
- Rate limit, audit, health, readiness/liveness ve metrics dusunulmeli.
- Kubernetes yetkileri en az yetki prensibine gore verilmeli.

## Proaktif Oneri Standardi

Codex, kullanici sadece tek bir degisiklik istese bile gelecekte ise yarayabilecek makul onerileri fark etmeli ve gerekiyorsa sormalidir.
Bu oneriler kullaniciyi yormadan, secilebilir ve amaca bagli olmalidir.

Oneri sorulabilecek durumlar:

- Yeni model ileride buyuyebilir ve ayrica DTO, enum, validator veya mapper gerektirebilir.
- Yeni mikroservis ileride Kafka, gRPC, CQRS veya admin audit ile entegre edilebilir.
- Bir endpoint sadece bugunku ihtiyaci karsiliyor ama ileride pagination, filtering, sorting veya search ihtiyaci dogurabilir.
- Bir entity icin database index, unique constraint, optimistic locking veya audit alanlari gerekli olabilir.
- Bir Kafka eventi icin idempotency, retry, dead-letter topic veya event versioning ihtiyaci olabilir.
- Bir controller public/admin olarak ayrilmali ya da role bazli yetki daha net tanimlanmali olabilir.
- Bir degisiklik ortak contract'i etkiliyorsa proto, DTO, topic adi veya response formatinda backward compatibility dusunulmelidir.
- Bir servis buyudukce shared utility, common error model, parent pom veya ortak test fixture faydali olabilir.
- Bir feature money, transaction, user veya admin gibi birden cok bounded context'e dokunuyorsa veri akisi once netlestirilmelidir.

## Servisler Arasi DTO Contract Kurali

- Ayni Kafka topic'i, gRPC metodu veya servisler arasi API ile tasinan ayni
  kavram; producer ve consumer tarafinda ayni alan adi, veri tipi, enum degeri,
  null davranisi, birim ve is anlamina sahip olmalidir.
- Kontrol sadece sinif adina gore yapilmaz. Serilestirilen wire semasi ve alanin
  domain anlami karsilastirilir.
- Nedensiz fark bulunursa tek canonical contract secilir; producer ve consumer
  bu contract'a getirilir ve her iki tarafta contract testi eklenir.
- Eski mesajlar kuyrukta bulunabilecekse alan silme veya yeniden adlandirma
  dogrudan kirici yapilmaz. Legacy alanlar gecici alias ile okunur, yeni
  mesajlarda yalnizca canonical alan yazilir.
- Event DTO'suna JPA entity veya tum history listesi koyulmaz. Yalnizca alicinin
  ihtiyac duydugu immutable snapshot alanlari tasinir.
- Ayni isimli ama bounded-context'e ozel request/response DTO'lari otomatik
  olarak ortaklastirilmaz. Fark is kuralindan kaynaklaniyorsa belgelenerek korunur.
- Consumer'in local idempotency veya processing state enum'u wire contract
  degildir; her serviste kasitli olarak farkli olabilir.
- Record, class veya farkli Java package kullanimi tek basina uyumsuzluk
  degildir. Wire alanlari ve anlamlari ayniysa temsil farki kabul edilebilir.
- Gson'dan Jackson'a gecis gibi serializer degisikligi tum topicler icin tek
  adimda yapilmaz. Once dual-compatible contract, test ve eski mesaj okuma
  garantisi kurulur; sonra kontrollu migrasyon yapilir.
- Transaction ve saga Kafka wire contract'larinin tek kaynagi
  `banking-contracts/src/main/proto/transaction-workflow-v1.proto` dosyasidir.
  Dort servis generated contract siniflarini kullanir; local
  `TransactionWorkflowState` yalniz servis ici mutable durumdur ve wire DTO
  olarak kabul edilmez.
- Contract degisikliginde once `banking-contracts` build ve compatibility
  testleri, sonra bagimli servis testleri calistirilmalidir.

Oneri sorma sekli:

- "Bunu ekleyebilirim" yerine "Bu degisiklik icin su 3 ekleme mantikli: pagination, audit event, admin endpoint. Hangilerini ekleyeyim?" gibi somut secenekler sunulmalidir.
- Riskli veya buyuk mimari kararlar kullaniciya sorulmalidir; kucuk, bariz ve mevcut pattern'e uygun kalite iyilestirmeleri dogrudan uygulanabilir.
- Sorular kisa, teknik sonucu belli ve secim yapmayi kolaylastiran sekilde olmali.
- Kullanici onay verirse Codex yalnizca secilenleri degil, onlarin gerektirdigi validation, exception, logging, test ve config ayrintilarini da tamamlamalidir.

Gereksiz soru sormama kurali:

- Kararin sonucu kod yapisini, veri akisini, guvenligi, public API'yi veya mimari sorumlulugu degistirmiyorsa soru sorulmamali.
- Mevcut projede ayni pattern zaten netse Codex o pattern'i takip edip ilerlemelidir.
- Kullanici hizli ve net bir is istemisse, yalnizca gercek risk veya onemli tercih varsa durup sormalidir.
- Sorulan her soru kullaniciya zaman kazandirmali; sadece emin olmak icin sorulan dusuk degerli sorulardan kacınılmalidir.
- Birden fazla soru gerekiyorsa bunlar tek seferde, secenekli ve kisa sunulmalidir.

## Guncellenebilirlik ve Surdurulebilirlik Kontrolu

Her yeni kod veya refactor sonrasinda Codex su sorulari kendi icinde kontrol etmelidir:

- Bu kod yeni alan eklenince kolay bozulur mu?
- DTO ve entity birbirine fazla baglandi mi?
- Response modeli ileride geriye donuk uyumlulugu koruyabilecek mi?
- Database tarafi migration, index veya unique constraint ister mi?
- Kafka eventi tekrar geldiginde para veya state iki kez degisir mi?
- Yeni enum/status eklenirse switch/if bloklari guvenli davranir mi?
- Service sinifi fazla sorumluluk aldiysa helper/component ayrimi gerekiyor mu?
- Testler sadece happy path'i mi kapsiyor, yoksa hata ve edge case de var mi?
- Loglar operasyon ekibinin bir request/event'i takip etmesine yetiyor mu?
- Bu degisiklik admin panelde gorunmeli, auditlenmeli veya reconciliation'a dahil edilmeli mi?
- Secret, token, password veya kullaniciya ait hassas bilgi loglara ya da manifestlere sizdi mi?
- Kubernetes, Docker, application.yaml veya gateway route guncellemesi gerekiyor mu?

Guncellenebilirlik icin varsayilan tercih:

- Geriye donuk uyumlu DTO alanlari.
- Version'li Kafka topic ve event payload'lari.
- Net requestId/eventUUID/correlationId tasarimi.
- Repository seviyesinde anlamli query method isimleri.
- Service seviyesinde kucuk ve test edilebilir metodlar.
- Controller seviyesinde sade endpointler ve dogru HTTP status kodlari.
- Exception seviyesinde domain'e ozel, okunabilir hata tipleri.
- Testlerde success, validation failure, not found, duplicate/idempotency ve insufficient funds gibi kritik senaryolar.

## Kod Stili

Yeni kod mevcut projeyle uyumlu olmalidir:

- Package yapisi `com.banking_microservices.<service_name>` standardini izlemeli.
- Lombok mevcut servislerde kullanildigi icin uygun yerde `@RequiredArgsConstructor`, `@Builder`, `@Getter`, `@Setter` kullanilabilir.
- Controller, service, repository, dto, model, exception paketleri net ayrilmali.
- GlobalExceptionHandler her serviste bulunmali ve benzer response formatlari kullanilmali.
- Loglar metod girisi, karar noktasi, basari ve hata noktalarinda anlamli olmali.
- Gereksiz abstraction eklenmemeli; once mevcut pattern takip edilmeli.
- Kod degisiklikleri dar kapsamli tutulmali, kullanicinin mevcut degisiklikleri geri alinmamali.

## Kullanici Ile Calisma Sekli

Codex'in bu projedeki davranisi:

- Kullanici ayrinti vermese bile proje amacina uygun kurumsal kalite dusunulmeli.
- Eksik veya riskli karar varsa durup kisa ve somut seceneklerle sorulmali.
- Mekanik dosya uretimi yerine veri akisi ve sorumluluk sinirlari kurulmalidir.
- Kullanici "servis/controller sonra" derse once temel domain iskeleti tamamlanmalidir.
- Kullanici onay verdikten sonra controller, service, Kafka/gRPC ve testler tamamlanmalidir.
- Final cevapta yapilan is, dokunulan ana dosyalar ve test durumu kisa anlatilmalidir.

## Kisa Hatirlatici

Bu proje icin Codex'in varsayilani sudur:

"Once mevcut mikroservis pattern'lerini oku, sonra model/repository/exception temelini kur, servis-controller onerilerini kullaniciya secenekli sor, onaydan sonra kurumsal bankacilik aklini kullanarak validation, logging, idempotency, transaction, security, audit ve test ayrintilarini tamamla."

## Customer Domain Context

Customer domaini uc servisli CQRS yapisindadir:

- `customer-service-command` PostgreSQL source-of-truth write servisidir.
- `customer-service-query` Kafka projectionlarini MongoDB read modeline ve Elasticsearch indexine yazar.
- `customer-service` gateway arkasindaki facade'dir; command ve query servisleriyle gRPC haberlesir.

User register akisi su sirayi korumali:

1. Keycloak kullanicisini olustur.
2. customer-service onboarding gRPC endpointi ile customer kaydini olustur.
3. Customer write basarisizsa Keycloak kullanicisini compensation olarak sil ve money account create eventini yayinlama.
4. Customer write basariliysa mevcut user-created Kafka eventini yayinla; money-service mevcut akisla hesabi olusturmaya devam eder.

Customer command/query/onboarding protobuf contractlari iki ucta ayni field numaralariyla tutulmalidir. Java package farki kabul edilebilir, wire semasi farkli olamaz.

`/api/customer-service/v1/customers/me` altindaki write endpointleri, request body'deki customer id yerine JWT'den gateway tarafindan gelen Keycloak id ile sahiplik dogrulamasi yapmalidir.
