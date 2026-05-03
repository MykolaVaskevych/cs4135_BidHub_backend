package com.bidhub.payment.service;

import com.bidhub.payment.dto.*;
import com.bidhub.payment.exception.InsufficientFundsException;
import com.bidhub.payment.exception.WalletNotFoundException;
import com.bidhub.payment.model.Transaction;
import com.bidhub.payment.model.TransactionType;
import com.bidhub.payment.model.Wallet;
import com.bidhub.payment.repository.TransactionRepository;
import com.bidhub.payment.repository.WalletRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public WalletResponse getOrCreateWallet(UUID userId) {
        Wallet wallet =
                walletRepository
                        .findByUserId(userId)
                        .orElseGet(
                                () ->
                                        walletRepository.save(
                                                Wallet.builder().userId(userId).build()));
        return toResponse(wallet);
    }

    @Transactional
    public WalletResponse topUp(UUID userId, TopUpRequest request) {
        Wallet wallet =
                walletRepository
                        .findByUserId(userId)
                        .orElseGet(
                                () ->
                                        walletRepository.save(
                                                Wallet.builder().userId(userId).build()));

        wallet.topUp(request.amount());

        transactionRepository.save(
                Transaction.builder()
                        .wallet(wallet)
                        .amount(request.amount())
                        .type(TransactionType.TOP_UP)
                        .description("Wallet top-up")
                        .build());

        return toResponse(walletRepository.save(wallet));
    }

    @Transactional
    public WalletResponse deduct(UUID userId, DeductRequest request) {
        ChargeOutcome outcome =
                chargeInternal(userId, request.amount(), request.description(), "Payment deduction");
        return toResponse(outcome.wallet());
    }

    @Transactional
    public ChargeResponse charge(UUID userId, ChargeRequest request) {
        ChargeOutcome outcome =
                chargeInternal(userId, request.amount(), request.description(), "Payment charge");
        Wallet wallet = outcome.wallet();
        return new ChargeResponse(
                outcome.transaction().getTransactionId(),
                wallet.getWalletId(),
                wallet.getUserId(),
                wallet.getBalance(),
                wallet.getCurrency());
    }

    private ChargeOutcome chargeInternal(
            UUID userId, BigDecimal amount, String description, String fallbackDescription) {
        Wallet wallet =
                walletRepository
                        .findByUserId(userId)
                        .orElseThrow(
                                () ->
                                        new WalletNotFoundException(
                                                "Wallet not found for user: " + userId));

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient funds. Balance: " + wallet.getBalance());
        }

        wallet.deduct(amount);

        Transaction tx =
                transactionRepository.save(
                        Transaction.builder()
                                .wallet(wallet)
                                .amount(amount)
                                .type(TransactionType.PAYMENT)
                                .description(description != null ? description : fallbackDescription)
                                .build());

        return new ChargeOutcome(walletRepository.save(wallet), tx);
    }

    private record ChargeOutcome(Wallet wallet, Transaction transaction) {}

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionHistory(UUID userId) {
        if (!walletRepository.existsByUserId(userId)) {
            throw new WalletNotFoundException("Wallet not found for user: " + userId);
        }
        return transactionRepository.findByWalletUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toTxResponse)
                .toList();
    }

    private WalletResponse toResponse(Wallet wallet) {
        return new WalletResponse(
                wallet.getWalletId(),
                wallet.getUserId(),
                wallet.getBalance(),
                wallet.getCurrency());
    }

    private TransactionResponse toTxResponse(Transaction tx) {
        return new TransactionResponse(
                tx.getTransactionId(),
                tx.getAmount(),
                tx.getType().name(),
                tx.getDescription(),
                tx.getCreatedAt());
    }
}
