# SpringBank Architecture Intelligence

Bu dizin, mimariyi elle cizilen ve hizla eskiyen tek bir diyagram yerine koddan,
konfigurasyondan ve acikca tanimlanmis is akislarindan yeniden uretilebilir hale
getirmek icin ayrilmistir.

Kurumsal hedef mimari, tum ana bankacilik domainleri ve 37 uctan uca referans akis
repo kokundeki `HIGH_SCALE_BANKING_REFERENCE_ARCHITECTURE.md` belgesinde
tanimlanmistir. Mevcut SpringBank gercegi ve hedef arasindaki gap analizi ise
`ENTERPRISE_BANKING_ARCHITECTURE_ASSESSMENT.md` belgesindedir.

## Kaynaklar

- `catalog/services.yaml`: Servis sahipligi, portlar, veri depolari ve iletisim kanallari.
- `catalog/flows.yaml`: Kritik is akislarinin adimlari, hata yollari ve tutarlilik sinirlari.
- `jqassistant/rules/springbank-rules.xml`: Java paket ve katman bagimliliklarini kontrol eden kurallar.
- `queries/architecture-inventory.cypher`: jQAssistant/Neo4j uzerinden servis ve kod envanteri sorgulari.
- `eventcatalog/`: Insanlar ve yapay zeka araclari icin aranabilir yasayan mimari portali.

## Arac Karari

Statik kod gercegi icin jQAssistant ve Neo4j, is akisi ve sozlesme katalogu icin
EventCatalog, calisma zamani gercegi icin OpenTelemetry ve SigNoz, gelistirme/test
seviyesinde metod izleri icin AppMap kullanilacaktir. Draw.io bir sunum ciktisidir;
mimarinin ana kaynagi degildir.

## Calistirma Sirasi

1. Tum Maven servislerini derleyin.
2. jQAssistant CLI ile servislerin `target/classes` dizinlerini tarayin.
3. `springbank:baseline` kural grubunu calistirin.
4. EventCatalog dizininde bagimliliklari kurup katalog gelistirme sunucusunu baslatin.
5. Uygulama ortaminda OpenTelemetry Java Agent ile izleri OTLP collector'a gonderin.

Komutlar ve surumler bilerek merkezi build altyapisi olusturulduktan sonra CI'a
sabitlenecektir. Projede su an ortak parent POM bulunmadigi icin on uc ayri Maven
build'ine ayni eklentiyi kopyalamak yerine once bu merkezi katman kurulmustur.

EventCatalog `Node.js >= 22.12.0` gerektirir. Daha eski Node surumleri Astro build
asamasinda desteklenmez. Katalog linter'indaki eksik event schema uyarilari mevcut
kodun gercek contract eksigini bilerek gorunur tutar; semalar uretilmeden bastirilmaz.

Tarama scripti jQAssistant/XO'nun JavaBean oneklerini makine diline gore farkli
yorumlamamasi icin JVM dilini `en-US` olarak sabitler. Bu ayar uygulamanin calisma
zamanini etkilemez; yalnizca mimari analiz prosesine uygulanir.

## Yonetisim

- Katalogdaki her servis bir bounded context, sahip ekip, veri sahibi ve kritikligi belirtmelidir.
- Her senkron cagrida timeout, kimlik dogrulama ve hata sozlesmesi bulunmalidir.
- Her asenkron mesajda sema surumu, uretici, tuketici, partition anahtari ve tekrar-isleme kurali bulunmalidir.
- Kod ile katalog farklilastiginda CI basarisiz olmali; katalog elle gercegin yerine gecmemelidir.
- Yeni bir servis acmadan once mevcut bir akis ve veri sahibi olup olmadigi aranmalidir.
