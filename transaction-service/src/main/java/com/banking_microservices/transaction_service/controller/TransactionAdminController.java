package com.banking_microservices.transaction_service.controller;

import com.banking_microservices.transaction_service.dto.enums.TransactionStatus;
import com.banking_microservices.transaction_service.dto.enums.TransactionType;
import com.banking_microservices.transaction_service.model.TransactionEntity;
import com.banking_microservices.transaction_service.repository.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Supplier;

/**
 * Admin paneli icin transaction istatistik ve yonetim endpoint'leri.
 * Gateway SecurityConfig: /api/transaction-service/v1/admin/** → hasRole("ADMIN")
 */
@Slf4j
@RestController
@RequestMapping("/api/transaction-service/v1/admin")
public class TransactionAdminController {


    private final TransactionRepository transactionRepository;
    private final Supplier<String> currentTime;

    public TransactionAdminController(TransactionRepository transactionRepository,
                                      Supplier<String> currentTime) {
        this.transactionRepository = transactionRepository;
        this.currentTime = currentTime;
    }

    /**
     * Toplam islem sayisi, hacim, tip bazli dagilim ozeti.
     */
    @GetMapping("/stats/summary")
    public ResponseEntity<?> getSummary() {
        log.info(" ({}) > TransactionAdminController | getSummary -> Istek alindi.", currentTime.get());

        List<TransactionEntity> all = transactionRepository.findAll();

        long totalCount = all.size();
        long completed  = 0;
        long failed     = 0;
        long pending    = 0;
        long depositCount  = 0;
        long withdrawCount = 0;
        long transferCount = 0;
        BigDecimal totalVolume   = BigDecimal.ZERO;
        BigDecimal depositVolume = BigDecimal.ZERO;
        BigDecimal withdrawVolume= BigDecimal.ZERO;
        BigDecimal transferVolume= BigDecimal.ZERO;

        for (TransactionEntity t : all) {
            if (TransactionStatus.COMPLETED.equals(t.getStatus())) {
                completed++;
            } else if (Boolean.TRUE.equals(t.getError())) {
                failed++;
            } else {
                pending++;
            }

            if (t.getMoney() != null) {
                totalVolume = totalVolume.add(t.getMoney());
            }

            if (TransactionType.DEPOSIT.equals(t.getTransactionType())) {
                depositCount++;
                if (t.getMoney() != null) depositVolume = depositVolume.add(t.getMoney());
            } else if (TransactionType.WITHDRAW.equals(t.getTransactionType())) {
                withdrawCount++;
                if (t.getMoney() != null) withdrawVolume = withdrawVolume.add(t.getMoney());
            } else if (TransactionType.TRANSFER.equals(t.getTransactionType())) {
                transferCount++;
                if (t.getMoney() != null) transferVolume = transferVolume.add(t.getMoney());
            }
        }

        Map<String, Object> countByType = new LinkedHashMap<>();
        countByType.put("DEPOSIT",  depositCount);
        countByType.put("WITHDRAW", withdrawCount);
        countByType.put("TRANSFER", transferCount);

        Map<String, Object> volumeByType = new LinkedHashMap<>();
        volumeByType.put("DEPOSIT",  depositVolume);
        volumeByType.put("WITHDRAW", withdrawVolume);
        volumeByType.put("TRANSFER", transferVolume);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalCount",   totalCount);
        result.put("totalVolume",  totalVolume);
        result.put("completed",    completed);
        result.put("pending",      pending);
        result.put("failed",       failed);
        result.put("countByType",  countByType);
        result.put("volumeByType", volumeByType);

        log.info(" ({}) > TransactionAdminController | getSummary -> Tamamlandi. Toplam: {}", currentTime.get(), totalCount);
        return ResponseEntity.ok(result);
    }

    /**
     * Son N gunluk gunluk islem sayilari (grafik icin).
     */
    @GetMapping("/stats/daily")
    public ResponseEntity<?> getDailyStats(@RequestParam(defaultValue = "30") int days) {
        log.info(" ({}) > TransactionAdminController | getDailyStats -> Istek alindi. Days: {}", currentTime.get(), days);

        LocalDateTime end   = LocalDateTime.now();
        LocalDateTime start = end.minusDays(days);
        List<TransactionEntity> txs = transactionRepository
                .findByLocalDateTimeBetweenOrderByLocalDateTimeDesc(start, end);

        Map<String, Map<String, Object>> grouped = new LinkedHashMap<>();
        for (int i = days - 1; i >= 0; i--) {
            String key = end.minusDays(i).toLocalDate().toString();
            Map<String, Object> day = new LinkedHashMap<>();
            day.put("date",   key);
            day.put("count",  0L);
            day.put("volume", BigDecimal.ZERO);
            day.put("errors", 0L);
            grouped.put(key, day);
        }

        for (TransactionEntity t : txs) {
            if (t.getLocalDateTime() == null) continue;
            String key = t.getLocalDateTime().toLocalDate().toString();
            if (!grouped.containsKey(key)) continue;

            Map<String, Object> day = grouped.get(key);
            day.put("count", (Long) day.get("count") + 1);

            if (t.getMoney() != null) {
                day.put("volume", ((BigDecimal) day.get("volume")).add(t.getMoney()));
            }
            if (Boolean.TRUE.equals(t.getError())) {
                day.put("errors", (Long) day.get("errors") + 1);
            }
        }

        log.info(" ({}) > TransactionAdminController | getDailyStats -> Tamamlandi.", currentTime.get());
        return ResponseEntity.ok(new ArrayList<>(grouped.values()));
    }

    /**
     * Takili kalan CREATED islemler (X dakikadan eski).
     */
    @GetMapping("/stuck")
    public ResponseEntity<?> getStuckTransactions(@RequestParam(defaultValue = "30") int olderThanMinutes) {
        log.info(" ({}) > TransactionAdminController | getStuckTransactions -> Istek alindi. OlderThan: {} dk", currentTime.get(), olderThanMinutes);

        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(olderThanMinutes);
        List<TransactionEntity> all = transactionRepository.findAll();
        List<TransactionEntity> stuck = new ArrayList<>();

        for (TransactionEntity t : all) {
            if (TransactionStatus.CREATED.equals(t.getStatus())
                    && t.getLocalDateTime() != null
                    && t.getLocalDateTime().isBefore(cutoff)) {
                stuck.add(t);
            }
        }

        stuck.sort(Comparator.comparing(TransactionEntity::getLocalDateTime));
        log.info(" ({}) > TransactionAdminController | getStuckTransactions -> Takili islem sayisi: {}", currentTime.get(), stuck.size());
        return ResponseEntity.ok(stuck);
    }

    /**
     * Belirli bir IBAN'in tum islemleri.
     */
    @GetMapping("/byiban")
    public ResponseEntity<?> getByIban(@RequestParam String iban) {
        log.info(" ({}) > TransactionAdminController | getByIban -> Istek alindi. IBAN: {}", currentTime.get(), iban);
        List<TransactionEntity> result = transactionRepository
                .findBySenderIbanOrReceiverIbanOrderByLocalDateTimeDesc(iban, iban);
        log.info(" ({}) > TransactionAdminController | getByIban -> Bulunan: {}", currentTime.get(), result.size());
        return ResponseEntity.ok(result);
    }

    /**
     * Hata analizi: tip bazli hata sayilari ve toplam kaybedilen hacim.
     */
    @GetMapping("/errors/analysis")
    public ResponseEntity<?> getErrorAnalysis() {
        log.info(" ({}) > TransactionAdminController | getErrorAnalysis -> Istek alindi.", currentTime.get());

        List<TransactionEntity> errors = transactionRepository.findByErrorTrue();
        BigDecimal totalLostVolume = BigDecimal.ZERO;
        long depositErrors  = 0;
        long withdrawErrors = 0;
        long transferErrors = 0;

        for (TransactionEntity e : errors) {
            if (e.getMoney() != null) {
                totalLostVolume = totalLostVolume.add(e.getMoney());
            }
            if (TransactionType.DEPOSIT.equals(e.getTransactionType()))  depositErrors++;
            else if (TransactionType.WITHDRAW.equals(e.getTransactionType())) withdrawErrors++;
            else if (TransactionType.TRANSFER.equals(e.getTransactionType())) transferErrors++;
        }

        Map<String, Object> byType = new LinkedHashMap<>();
        byType.put("DEPOSIT",  depositErrors);
        byType.put("WITHDRAW", withdrawErrors);
        byType.put("TRANSFER", transferErrors);

        List<TransactionEntity> recent = new ArrayList<>();
        errors.sort(Comparator.comparing(
                t -> t.getLocalDateTime() != null ? t.getLocalDateTime() : LocalDateTime.MIN,
                Comparator.reverseOrder()));
        for (int i = 0; i < Math.min(5, errors.size()); i++) {
            recent.add(errors.get(i));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalErrors",     errors.size());
        result.put("errorsByType",    byType);
        result.put("totalLostVolume", totalLostVolume);
        result.put("recentErrors",    recent);

        log.info(" ({}) > TransactionAdminController | getErrorAnalysis -> Toplam hata: {}", currentTime.get(), errors.size());
        return ResponseEntity.ok(result);
    }

    /**
     * Toplam islem ve hata sayisi.
     */
    @GetMapping("/stats/count")
    public ResponseEntity<?> getCount() {
        log.info(" ({}) > TransactionAdminController | getCount -> Istek alindi.", currentTime.get());
        long total  = transactionRepository.count();
        long errors = transactionRepository.findByErrorTrue().size();
        Map<String, Long> result = new LinkedHashMap<>();
        result.put("total",  total);
        result.put("errors", errors);
        log.info(" ({}) > TransactionAdminController | getCount -> Total: {}, Errors: {}", currentTime.get(), total, errors);
        return ResponseEntity.ok(result);
    }

    /**
     * En buyuk hacimli N islem.
     */
    @GetMapping("/stats/top-by-volume")
    public ResponseEntity<?> getTopByVolume(@RequestParam(defaultValue = "10") int limit) {
        log.info(" ({}) > TransactionAdminController | getTopByVolume -> Istek alindi. Limit: {}", currentTime.get(), limit);
        List<TransactionEntity> all = transactionRepository.findAll();
        List<TransactionEntity> sorted = new ArrayList<>();

        for (TransactionEntity t : all) {
            if (t.getMoney() != null) sorted.add(t);
        }
        sorted.sort(Comparator.comparing(TransactionEntity::getMoney).reversed());

        List<TransactionEntity> result = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, sorted.size()); i++) {
            result.add(sorted.get(i));
        }

        log.info(" ({}) > TransactionAdminController | getTopByVolume -> Donduruldu: {}", currentTime.get(), result.size());
        return ResponseEntity.ok(result);
    }
}
