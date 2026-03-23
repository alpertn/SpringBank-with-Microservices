package com.banking_microservices.money_service.service;

import com.banking_microservices.money_service.dto.MoneyDto;
import com.banking_microservices.money_service.exception.SaveUserException;
import com.banking_microservices.money_service.models.UserMoney;
import com.banking_microservices.money_service.repository.UserMoneyRepository;
import com.banking_microservices.money_service.service.helper.AccountQueryService;
import com.banking_microservices.money_service.service.helper.BalanceOperationService;
import com.banking_microservices.money_service.service.helper.IbanGeneratorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.function.Supplier;

/**
 * Bu class {@link IbanGeneratorService}, {@link AccountQueryService} ve {@link BalanceOperationService} classlarini cagirir.
 *
 * Kullanici para hesabi islemleri icin ana orkestrasyon servisidir.
 * IBAN uretimi, hesap sorgulama ve bakiye islemleri ilgili helper classlara delege edilir.
 *
 * Controller ve Kafka listenerlar bu classi cagirir.
 * Orijinal method imzalari korundugu icin cagiran taraflar bozulmaz.
 */
@Slf4j
@Service
public class UserMoneyService {

    private final UserMoneyRepository userMoneyRepository;
    private final IbanGeneratorService ibanGeneratorService;
    private final AccountQueryService accountQueryService;
    private final BalanceOperationService balanceOperationService;
    private final Supplier<String> currentTime;

    public UserMoneyService(UserMoneyRepository userMoneyRepository,
                            IbanGeneratorService ibanGeneratorService,
                            AccountQueryService accountQueryService,
                            BalanceOperationService balanceOperationService,
                            Supplier<String> currentTime) {
        this.userMoneyRepository = userMoneyRepository;
        this.ibanGeneratorService = ibanGeneratorService;
        this.accountQueryService = accountQueryService;
        this.balanceOperationService = balanceOperationService;
        this.currentTime = currentTime;
    }

    /**
     * userId ile yeni bir {@link UserMoney} hesabi olusturur ve kaydeder.
     *
     * @param userId keycloak uuid ve veritabanindaki userId degeri
     * @return olusturulan {@link UserMoney} nesnesi
     * @throws SaveUserException kayit basarisizsa firlatir
     */
    public UserMoney generateUser(String userId) {
        log.info(" ({}) > UserMoneyService | generateUser -> generateUser istegi alindi. UserId: {}", currentTime.get(), userId);

        UserMoney userMoney = UserMoney.builder()
                .userId(userId)
                .userIban(ibanGeneratorService.generateUniqueTurkishIban())
                .build();

        try {
            UserMoney savedUserMoney = userMoneyRepository.save(userMoney);

            log.info(" ({}) > UserMoneyService | generateUser -> Kullanici UserMoney servisine kaydedildi. {}", currentTime.get(), savedUserMoney);
            return savedUserMoney;
        } catch (Exception e) {
            log.error(" ({}) > UserMoneyService | generateUser -> Veritabanina kayit sirasinda hata olustu! UserId: {}, Hata: {}", currentTime.get(), userId, e.getMessage());
            throw new SaveUserException("Failed to save user on UserMoney-Service " + userId);
        }
    }

    /**
     * Benzersiz Turkiye IBANi uretir.
     * Geriye donuk uyumluluk icin korundu.
     *
     * @return yeni ve benzersiz IBAN string
     */
    public String generateRandomTurkishIban() {
        return ibanGeneratorService.generateUniqueTurkishIban();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HESAP SORGULAMA  AccountQueryService'e delege
    // ─────────────────────────────────────────────────────────────────────────

    public MoneyDto getAccountById(String id) { return accountQueryService.getAccountById(id); }

    public MoneyDto getAccountByIban(String iban) { return accountQueryService.getAccountByIban(iban); }

    public MoneyDto getAccountByUserId(String userId) { return accountQueryService.getAccountByUserId(userId); }

    public BigDecimal getBalanceById(String id) { return accountQueryService.getBalanceById(id); }

    public BigDecimal getBalanceByIban(String iban) { return accountQueryService.getBalanceByIban(iban); }

    public BigDecimal getBalanceByUserId(String userId) { return accountQueryService.getBalanceByUserId(userId); }

    // ─────────────────────────────────────────────────────────────────────────
    // BAKİYE ISLEMLERI  BalanceOperationService'e delege
    // ─────────────────────────────────────────────────────────────────────────

    public void depositMoneyById(String id, BigDecimal amount) { balanceOperationService.depositMoneyById(id, amount); }

    public void depositMoneyByIban(String iban, BigDecimal amount) { balanceOperationService.depositMoneyByIban(iban, amount); }

    public void depositMoneyByUserId(String userId, BigDecimal amount) { balanceOperationService.depositMoneyByUserId(userId, amount); }

    public void withdrawMoneyById(String id, BigDecimal amount) { balanceOperationService.withdrawMoneyById(id, amount); }

    public void withdrawMoneyByIban(String iban, BigDecimal amount) { balanceOperationService.withdrawMoneyByIban(iban, amount); }

    public void withdrawMoneyByUserId(String userId, BigDecimal amount) { balanceOperationService.withdrawMoneyByUserId(userId, amount); }
}