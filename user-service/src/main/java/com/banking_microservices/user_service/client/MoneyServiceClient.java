package com.banking_microservices.user_service.client;

import com.banking_microservices.user_service.dto.user.IdDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "money-service")
public interface MoneyServiceClient {

    @PostMapping("/api/createusermoney")
    ResponseEntity<Object> createUser(@RequestBody IdDto userId);

}

// import org.springframework.cloud.openfeign.FeignClient;
// import org.springframework.web.bind.annotation.PostMapping;
// import org.springframework.web.bind.annotation.RequestBody;
// import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
// import io.github.resilience4j.retry.annotation.Retry;
// import lombok.AllArgsConstructor;
// import lombok.Data;
// import lombok.NoArgsConstructor;
// import java.math.BigDecimal;
// import java.util.UUID;
//
// @FeignClient(name = "money-UserService", url =
// "${money.UserService.url:http://localhost:8082}") // Feign client tanımı.
// Money-Service'e HTTP çağrısı yapar. application.yml'den URL alır. Default
// localhost:8082. UserService.createUser() tarafından kullanılır. Olmazsa hesap
// açılamaz
// public interface MoneyServiceClient {
//
// @PostMapping("/api/accounts") // POST /api/accounts endpoint'i. Yeni hesap
// oluşturur. UserService.createUser() içinde çağrılır. CreateAccountRequest
// gönderir. Olmazsa kullanıcı için hesap açılamaz
// @CircuitBreaker(name = "moneyService", fallbackMethod =
// "createAccountFallback") // Circuit breaker fault tolerance. Money-Service
// down olursa devreyi açar. 50% failure rate, 10 saniye wait.
// application-resilience.yml'den config alır. Olmazsa cascade failure olur
// @Retry(name = "moneyService") // Retry mekanizması. Başarısız isteklerde
// otomatik tekrar dener. 3 deneme, 500ms wait, exponential backoff.
// application-resilience.yml'den config alır. Olmazsa ilk hatada fail eder
// void createAccount(@RequestBody CreateAccountRequest request); // Hesap açma
// metodu. UserService'den çağrılır. UserId ve balance içeren request gönderir.
// Money-Service'de Account entity oluşturur. Olmazsa yeni kullanıcının hesabı
// olmaz
//
// default void createAccountFallback(CreateAccountRequest request, Exception
// ex) { // Fallback metodu. Circuit breaker ve retry başarısız olursa çağrılır.
// RuntimeException fırlatır. UserService transaction rollback yapar. Olmazsa
// kullanıcı kaydedilir ama hesap açılmaz
// throw new RuntimeException("Money Service is currently unavailable. Account
// creation will be retried later. User ID: " + request.getUserId(), ex); //
// RuntimeException fırlatır. UserService.createUser() içinde yakalanır.
// Transaction rollback olur. Kullanıcı ve hesap birlikte oluşturulur veya hiç
// oluşturulmaz
// }
//
// @Data // Lombok getter/setter/toString/equals/hashCode oluşturur. Jackson
// JSON serialization için kullanılır. Olmazsa manuel getter/setter gerekir
// @NoArgsConstructor // Lombok parametresiz constructor oluşturur. Jackson
// deserialize için gerekli. Olmazsa JSON parse hatası
// @AllArgsConstructor // Lombok tüm parametreli constructor oluşturur.
// UserService'de new CreateAccountRequest(userId, balance) kullanımı için.
// Olmazsa manuel set gerekir
// class CreateAccountRequest { // Inner class. Hesap açma request DTO'su.
// UserService'den Money-Service'e gönderilir. UserId ve balance içerir. Olmazsa
// veri gönderilemez
// private UUID userId; // Hesap sahibi user ID. User.getId()'den alınır.
// Money-Service'de Account.userId olarak kaydedilir. User-Account ilişkisi
// kurar. Olmazsa hesap sahibi bilinemez
// private BigDecimal balance; // Başlangıç bakiyesi. UserService'de 1000 TL
// olarak set edilir. Money-Service'de Account.balance olarak kaydedilir.
// Olmazsa bakiye 0 olur
// }
// }