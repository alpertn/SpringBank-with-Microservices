package com.banking_microservices.money_service.controller;

import com.banking_microservices.money_service.models.UserMoney;
import com.banking_microservices.money_service.repository.UserMoneyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Supplier;

/**
 * Admin paneli icin hesap istatistik ve yonetim endpoint'leri.
 * Gateway SecurityConfig: /api/money-service/v1/admin/** → hasRole("ADMIN")
 */
@Slf4j
@RestController
@RequestMapping("/api/money-service/v1/admin")
public class MoneyAdminController {


    private final UserMoneyRepository userMoneyRepository;
    private final Supplier<String> currentTime;

    public MoneyAdminController(UserMoneyRepository userMoneyRepository,
                                Supplier<String> currentTime) {
        this.userMoneyRepository = userMoneyRepository;
        this.currentTime = currentTime;
    }

    /**
     * Sistem geneli bakiye ozeti: toplam bakiye, bloke, ortalama vb.
     */
    @GetMapping("/stats/summary")
    public ResponseEntity<?> getSummary() {
        log.info(" ({}) > MoneyAdminController | getSummary -> Istek alindi.", currentTime.get());

        List<UserMoney> all = userMoneyRepository.findAll();

        BigDecimal totalBalance  = BigDecimal.ZERO;
        BigDecimal totalBlocked  = BigDecimal.ZERO;
        long richAccounts        = 0;
        long zeroBalance         = 0;
        long withBlocked         = 0;

        for (UserMoney u : all) {
            if (u.getMoney() != null) {
                totalBalance = totalBalance.add(u.getMoney());
                if (u.getMoney().compareTo(new BigDecimal("10000")) > 0) richAccounts++;
                if (u.getMoney().compareTo(BigDecimal.ZERO) == 0) zeroBalance++;
            } else {
                zeroBalance++;
            }
            if (u.getBlockedMoney() != null && u.getBlockedMoney().compareTo(BigDecimal.ZERO) > 0) {
                totalBlocked = totalBlocked.add(u.getBlockedMoney());
                withBlocked++;
            }
        }

        BigDecimal avgBalance = BigDecimal.ZERO;
        if (!all.isEmpty()) {
            avgBalance = totalBalance.divide(BigDecimal.valueOf(all.size()), 2, RoundingMode.HALF_UP);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalAccounts",          all.size());
        result.put("totalBalance",           totalBalance);
        result.put("totalBlockedBalance",    totalBlocked);
        result.put("averageBalance",         avgBalance);
        result.put("richAccounts",           richAccounts);
        result.put("zeroBalanceAccounts",    zeroBalance);
        result.put("accountsWithBlockedFunds", withBlocked);

        log.info(" ({}) > MoneyAdminController | getSummary -> Tamamlandi. TotalAccounts: {}, TotalBalance: {}", currentTime.get(), all.size(), totalBalance);
        return ResponseEntity.ok(result);
    }

    /**
     * Tum hesaplari listeler (admin icin).
     */
    @GetMapping("/accounts")
    public ResponseEntity<?> getAllAccounts(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {
        log.info(" ({}) > MoneyAdminController | getAllAccounts -> Istek alindi. Page: {}, Size: {}", currentTime.get(), page, size);

        List<UserMoney> all = userMoneyRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        int skip  = page * size;
        int count = 0;

        for (UserMoney u : all) {
            if (count < skip) { count++; continue; }
            if (result.size() >= size) break;

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",               u.getId());
            m.put("userId",           u.getUserId());
            m.put("keycloakUserUUID", u.getKeycloakUserUUID());
            m.put("userIban",         u.getUserIban());
            m.put("money",            u.getMoney());
            m.put("blockedMoney",     u.getBlockedMoney());
            result.add(m);
        }

        log.info(" ({}) > MoneyAdminController | getAllAccounts -> Donduruldu: {}", currentTime.get(), result.size());
        return ResponseEntity.ok(result);
    }

    /**
     * En yuksek bakiyeli N hesap.
     */
    @GetMapping("/accounts/top")
    public ResponseEntity<?> getTopAccounts(@RequestParam(defaultValue = "10") int limit) {
        log.info(" ({}) > MoneyAdminController | getTopAccounts -> Istek alindi. Limit: {}", currentTime.get(), limit);

        List<UserMoney> all = userMoneyRepository.findAll();
        List<UserMoney> withBalance = new ArrayList<>();
        for (UserMoney u : all) {
            if (u.getMoney() != null) withBalance.add(u);
        }
        withBalance.sort(Comparator.comparing(UserMoney::getMoney).reversed());

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, withBalance.size()); i++) {
            UserMoney u = withBalance.get(i);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userId",       u.getUserId());
            m.put("userIban",     u.getUserIban());
            m.put("balance",      u.getMoney());
            m.put("blockedMoney", u.getBlockedMoney());
            result.add(m);
        }

        log.info(" ({}) > MoneyAdminController | getTopAccounts -> Donduruldu: {}", currentTime.get(), result.size());
        return ResponseEntity.ok(result);
    }

    /**
     * Belirli bir IBAN icin hesap bilgisi.
     */
    @GetMapping("/account/byiban")
    public ResponseEntity<?> getAccountByIban(@RequestParam String iban) {
        log.info(" ({}) > MoneyAdminController | getAccountByIban -> Istek alindi. IBAN: {}", currentTime.get(), iban);

        Optional<UserMoney> found = userMoneyRepository.findByUserIban(iban);
        if (found.isEmpty()) {
            log.warn(" ({}) > MoneyAdminController | getAccountByIban -> IBAN bulunamadi: {}", currentTime.get(), iban);
            return ResponseEntity.notFound().build();
        }

        log.info(" ({}) > MoneyAdminController | getAccountByIban -> Bulundu. UserId: {}", currentTime.get(), found.get().getUserId());
        return ResponseEntity.ok(found.get());
    }

    /**
     * Admin bloke kaldir: kullanicinin bloke bakiyesini serbest birak.
     */
    @PostMapping("/account/unblock")
    public ResponseEntity<?> unblockFunds(@RequestBody Map<String, String> body) {
        String userId = body.get("userId");
        log.warn(" ({}) > MoneyAdminController | unblockFunds -> Admin bloke kaldiriyor. UserId: {}", currentTime.get(), userId);

        if (userId == null || userId.isBlank()) {
            log.warn(" ({}) > MoneyAdminController | unblockFunds -> userId bos geldi.", currentTime.get());
            return ResponseEntity.badRequest().body(Map.of("error", "userId gerekli"));
        }

        Optional<UserMoney> found = userMoneyRepository.findByUserId(userId);
        if (found.isEmpty()) {
            log.warn(" ({}) > MoneyAdminController | unblockFunds -> Kullanici bulunamadi: {}", currentTime.get(), userId);
            return ResponseEntity.notFound().build();
        }

        UserMoney acc     = found.get();
        BigDecimal blocked = acc.getBlockedMoney() != null ? acc.getBlockedMoney() : BigDecimal.ZERO;

        acc.setMoney(acc.getMoney() != null ? acc.getMoney().add(blocked) : blocked);
        acc.setBlockedMoney(BigDecimal.ZERO);
        userMoneyRepository.save(acc);

        log.info(" ({}) > MoneyAdminController | unblockFunds -> Serbest birakildi: {} TRY. UserId: {}", currentTime.get(), blocked, userId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status",     "success");
        result.put("unblocked",  blocked);
        result.put("newBalance", acc.getMoney());
        return ResponseEntity.ok(result);
    }

    /**
     * Bakiye araligina gore hesap dagilimi (grafik icin).
     */
    @GetMapping("/stats/distribution")
    public ResponseEntity<?> getBalanceDistribution() {
        log.info(" ({}) > MoneyAdminController | getBalanceDistribution -> Istek alindi.", currentTime.get());

        List<UserMoney> all = userMoneyRepository.findAll();
        long range0      = 0;
        long range1      = 0;
        long range1K     = 0;
        long range10K    = 0;
        long range50K    = 0;

        for (UserMoney u : all) {
            if (u.getMoney() == null || u.getMoney().compareTo(BigDecimal.ZERO) == 0) {
                range0++;
            } else if (u.getMoney().compareTo(new BigDecimal("1000")) < 0) {
                range1++;
            } else if (u.getMoney().compareTo(new BigDecimal("10000")) < 0) {
                range1K++;
            } else if (u.getMoney().compareTo(new BigDecimal("50000")) < 0) {
                range10K++;
            } else {
                range50K++;
            }
        }

        Map<String, Long> result = new LinkedHashMap<>();
        result.put("0 TL",       range0);
        result.put("1-999 TL",   range1);
        result.put("1K-10K TL",  range1K);
        result.put("10K-50K TL", range10K);
        result.put("50K+ TL",    range50K);

        log.info(" ({}) > MoneyAdminController | getBalanceDistribution -> Tamamlandi.", currentTime.get());
        return ResponseEntity.ok(result);
    }

    /**
     * Toplam hesap sayisi.
     */
    @GetMapping("/stats/count")
    public ResponseEntity<?> getCount() {
        log.info(" ({}) > MoneyAdminController | getCount -> Istek alindi.", currentTime.get());
        long total = userMoneyRepository.count();
        log.info(" ({}) > MoneyAdminController | getCount -> Toplam hesap: {}", currentTime.get(), total);
        return ResponseEntity.ok(Map.of("totalAccounts", total));
    }

    /**
     * Admin hesap silme: userId veya keycloakUserUUID ile money kaydini sil.
     */
    @DeleteMapping("/account/{userId}")
    public ResponseEntity<?> deleteAccount(@PathVariable String userId) {
        log.warn(" ({}) > MoneyAdminController | deleteAccount -> Admin hesap siliyor. UserId: {}", currentTime.get(), userId);

        Optional<UserMoney> found = userMoneyRepository.findByUserId(userId);
        if (found.isEmpty()) {
            // keycloakUserUUID ile de dene
            List<UserMoney> all = userMoneyRepository.findAll();
            found = all.stream()
                    .filter(u -> userId.equals(u.getKeycloakUserUUID()))
                    .findFirst();
        }

        if (found.isEmpty()) {
            log.warn(" ({}) > MoneyAdminController | deleteAccount -> Hesap bulunamadi: {}", currentTime.get(), userId);
            return ResponseEntity.notFound().build();
        }

        UserMoney acc = found.get();
        userMoneyRepository.delete(acc);
        log.info(" ({}) > MoneyAdminController | deleteAccount -> Hesap silindi. UserId: {}, IBAN: {}", currentTime.get(), acc.getUserId(), acc.getUserIban());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "deleted");
        result.put("userId", acc.getUserId());
        result.put("userIban", acc.getUserIban());
        result.put("deletedBalance", acc.getMoney());
        return ResponseEntity.ok(result);
    }
}
