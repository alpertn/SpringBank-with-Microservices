# Banking Contracts

Bu modul mikroservisler arasinda tasinan version'li Protobuf sozlesmelerinin tek
kaynagidir. JPA entity, repository, service veya is kurali bu module eklenmez.

## Mevcut sozlesmeler

- `transaction-workflow-v1.proto`: transaction workflow eventi, transaction ve
  saga enumlari, saga transaction snapshot'i ve saga workflow eventi.
- Java contract siniflari Maven build sirasinda otomatik uretilir.
- transaction-service, money-service, user-service ve fraud-service bu artifact'i
  dependency olarak kullanir.

## Degisiklik kurallari

- Mevcut field numarasi degistirilmez ve baska anlam icin tekrar kullanilmaz.
- Silinen alanin numarasi ve adi `reserved` olarak tutulur.
- Yeni alan yeni bir numarayla ve mumkunse `optional` olarak eklenir.
- Mevcut enum numarasi degistirilmez; yeni enum degeri sona eklenir.
- Anlami kirilan event icin mevcut v1 degistirilmez, yeni v2 contract olusturulur.
- Contract testi gecmeden artifact yayinlanmaz.

## Build

Repo kokundeki Maven reactor once bu modulu, sonra bagimli servisleri build eder.
CI ve ilgili Dockerfile'lar da servisten once bu artifact'i kurar.

Kafka transportu gecis asamasinda Protobuf generated contract'in canonical JSON
temsilini kullanir. Listener'lar topic retention suresi boyunca eski Gson
mesajlarini da okuyabilir. Bir sonraki asamada Schema Registry ile Protobuf binary
transportuna gecildiginde bu legacy fallback kaldirilabilir.
