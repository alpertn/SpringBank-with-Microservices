<h1 align="center">Merhaba, ben Alper 👋</h1>

<p align="center">
 13 Yaşından beri yazılım üzerine çalışıyorum. son 2 yıldır Java Spring Boot, Mikroservis Mimarisi, Dağınık sistemlerde veri akışı hakkında çalışıyorum.<br/> Her öğrendiğim yeni bir bilgiyi Repository veya olan bir repositorye commit atarak yayınlıyorum.
</p>

<p align="center">
  <a href="https://github.com/alpertn">
  </a>
    <img src="https://komarev.com/ghpvc/?username=alpertn&label=Profil%20Görüntülenme&color=6366f1&style=for-the-badge" alt="Profile Views" />
  </div>
</p>


---

<div align="center">

## 🛠️ Teknolojiler & Stack

**Backend**

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=spring-security&logoColor=white)
![Spring Gateway](https://img.shields.io/badge/Spring_Gateway-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Spring Data JPA](https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=for-the-badge&logo=spring&logoColor=white)

**Mikroservis İletişimi & Service Discovery**

![Eureka Server](https://img.shields.io/badge/Eureka_Server-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Feign Client](https://img.shields.io/badge/Feign_Client-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Kubernetes Service Discovery](https://img.shields.io/badge/K8s_Service_Discovery-326CE5?style=for-the-badge&logo=kubernetes&logoColor=white)

**Mikroservisler Arasi Anlasma**

![Apache Kafka](https://img.shields.io/badge/Apache_Kafka-231F20?style=for-the-badge&logo=apache-kafka&logoColor=white)

**Kimlik Doğrulama**

![Keycloak](https://img.shields.io/badge/Keycloak-4D4D4D?style=for-the-badge&logo=keycloak&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=json-web-tokens&logoColor=white)

**Veritabanı & Cache & Sql & NoSQL**

![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)

**Container ve Container Orkestra**

![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Kubernetes](https://img.shields.io/badge/Kubernetes-326CE5?style=for-the-badge&logo=kubernetes&logoColor=white)

**Gözlemlenebilirlik ve Loglama**

![Zipkin](https://img.shields.io/badge/Zipkin-FF6600?style=for-the-badge&logo=zipkin&logoColor=white)
![SLF4J](https://img.shields.io/badge/SLF4J-000000?style=for-the-badge&logo=scala&logoColor=white)
![Logback](https://img.shields.io/badge/Logback-6DB33F?style=for-the-badge&logo=spring&logoColor=white)

**Araçlar & IDE**

![IntelliJ IDEA](https://img.shields.io/badge/IntelliJ_IDEA-000000?style=for-the-badge&logo=intellij-idea&logoColor=white)
![Postman](https://img.shields.io/badge/Postman-FF6C37?style=for-the-badge&logo=postman&logoColor=white)
![cURL](https://img.shields.io/badge/cURL-073551?style=for-the-badge&logo=curl&logoColor=white)
![Gson](https://img.shields.io/badge/Gson-4285F4?style=for-the-badge&logo=google&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)
![Git](https://img.shields.io/badge/Git-F05032?style=for-the-badge&logo=git&logoColor=white)

**Diğer Diller**

![C#](https://img.shields.io/badge/C%23-239120?style=for-the-badge&logo=c-sharp&logoColor=white)
![Python](https://img.shields.io/badge/Python-3776AB?style=for-the-badge&logo=python&logoColor=white)
![HTML5](https://img.shields.io/badge/HTML5-E34F26?style=for-the-badge&logo=html5&logoColor=white)
![CSS3](https://img.shields.io/badge/CSS3-1572B6?style=for-the-badge&logo=css3&logoColor=white)

**Kullanılan Yapay Zeka Araçları**

![Claude](https://img.shields.io/badge/Claude_CODE-D97757?style=for-the-badge&logo=anthropic&logoColor=white)
![Gemini 1.5 Pro](https://img.shields.io/badge/Antigravity-4285F4?style=for-the-badge&logo=google-gemini&logoColor=white)


---

## Projelerin Minari Yapısı

###  [SpringBank-With-Microservices](https://github.com/alpertn/SpringBank-with-Microservices)

6 farklı ve bağımsız mikroservisden oluşan SpringBank-Wıth-Microservices projesi doğru mikroservis anlayışı, clean code, desing pattern ogrenimi ile alakali bana çok şey kattı. 

**Mikroservis Görev Dağılım ve Mimari**

| Servis | Sorumluluk |
|---|---|
| gateway | Klasik keycloak entegrasyonlu Spring Gateway.|
| auth-service | klasik token üretimi , create user ve Keycloak entegrasyonu. |
| user-service| klasik kullanıcı kaydı ve profil yönetimi. |
| money-service| Klasik money service işlemleri. |
| transaction-service | Transfer akışını create eden yer. Diğer topiclere gonderılmek uzere kafkaya ilk event atan yer. |
| fraud-service | Topicleri dinler matematiksel hesaplamalar yapar ve veri akışını devam ettirir. |

<h2 align="center">Proje Mimarisi</h1>

<p align="center">
  <strong>Transfer</strong><br/>
  <img src="https://raw.githubusercontent.com/alpertn/SpringBank-with-Microservices/main/readme/240326.png" alt="Transfer Architecture" width="800"/>
</p>

<div align="center">
<h4> Tıkla ve Mimariyi Draw.io'da Aç </h4>
</a>
<a href="https://app.diagrams.net/?#Uhttps://raw.githubusercontent.com/alpertn/SpringBank-with-Microservices/refs/heads/main/readme/240326.drawio" target="_blank">
  <img src="https://img.shields.io/badge/Open_in-Draw.io-orange?style=for-the-badge&logo=diagrams.net" alt="Draw.io'da Çizimi Aç">
</a>
</div>

---

<h1 align="center">GitHub İstatistikleri</h1>>

<p align="center">
  <img src="https://github-readme-streak-stats.herokuapp.com/?user=alpertn&theme=tokyonight&hide_border=true" alt="GitHub Streak" />
</p>
<p align="center">
  <img src="https://github-profile-summary-cards.vercel.app/api/cards/profile-details?username=alpertn&theme=tokyonight" alt="GitHub Profile Summary" />
</p>

---

