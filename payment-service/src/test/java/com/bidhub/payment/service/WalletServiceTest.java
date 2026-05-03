package com.bidhub.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bidhub.payment.dto.ChargeRequest;
import com.bidhub.payment.dto.ChargeResponse;
import com.bidhub.payment.exception.InsufficientFundsException;
import com.bidhub.payment.exception.WalletNotFoundException;
import com.bidhub.payment.model.Transaction;
import com.bidhub.payment.model.Wallet;
import com.bidhub.payment.repository.TransactionRepository;
import com.bidhub.payment.repository.WalletRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock private WalletRepository walletRepository;
    @Mock private TransactionRepository transactionRepository;
    @InjectMocks private WalletService walletService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID WALLET_ID = UUID.randomUUID();
    private static final UUID TRANSACTION_ID = UUID.randomUUID();

    @Test
    @DisplayName("charge: returns transactionId, walletId, userId, balance, currency on success")
    void charge_success_returnsTransactionId() {
        Wallet wallet = Wallet.builder()
                .walletId(WALLET_ID)
                .userId(USER_ID)
                .balance(BigDecimal.valueOf(100))
                .currency("EUR")
                .build();
        when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setTransactionId(TRANSACTION_ID);
            return t;
        });
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        ChargeResponse response =
                walletService.charge(
                        USER_ID,
                        new ChargeRequest(BigDecimal.valueOf(40), "auction-buy-now"));

        assertThat(response.transactionId()).isEqualTo(TRANSACTION_ID);
        assertThat(response.walletId()).isEqualTo(WALLET_ID);
        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.balance()).isEqualByComparingTo(BigDecimal.valueOf(60));
        assertThat(response.currency()).isEqualTo("EUR");
    }

    @Test
    @DisplayName("charge: throws InsufficientFundsException when balance < amount")
    void charge_insufficientBalance_throws() {
        Wallet wallet = Wallet.builder()
                .walletId(WALLET_ID)
                .userId(USER_ID)
                .balance(BigDecimal.valueOf(10))
                .currency("EUR")
                .build();
        when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));

        assertThatThrownBy(
                        () ->
                                walletService.charge(
                                        USER_ID,
                                        new ChargeRequest(BigDecimal.valueOf(40), "test")))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    @DisplayName("charge: throws WalletNotFoundException when wallet does not exist")
    void charge_walletMissing_throws() {
        when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                walletService.charge(
                                        USER_ID,
                                        new ChargeRequest(BigDecimal.valueOf(40), "test")))
                .isInstanceOf(WalletNotFoundException.class);
    }
}
