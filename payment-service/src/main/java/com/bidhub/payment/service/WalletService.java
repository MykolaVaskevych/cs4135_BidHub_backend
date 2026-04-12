package com.bidhub.payment.service;

import com.bidhub.payment.dto.*;
import com.bidhub.payment.exception.InsufficientFundsException;
import com.bidhub.payment.exception.WalletNotFoundException;
import com.bidhub.payment.model.Transaction;
import com.bidhub.payment.model.TransactionType;
import com.bidhub.payment.model.Wallet;
import com.bidhub.payment.repository.TransactionRepository;
import com.bidhub.payment.repository.WalletRepository;
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
        Wallet wallet =
                walletRepository
                        .findByUserId(userId)
                        .orElseThrow(
                                () ->
                                        new WalletNotFoundException(
                                                "Wallet not found for user: " + userId));

        if (wallet.getBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient funds. Balance: " + wallet.getBalance());
        }

        wallet.deduct(request.amount());

        transactionRepository.save(
                Transaction.builder()
                        .wallet(wallet)
                        .amount(request.amount())
                        .type(TransactionType.PAYMENT)
                        .description(
                                request.description() != null
                                        ? request.description()
                                        : "Payment deduction")
                        .build());

        return toResponse(walletRepository.save(wallet));
    }

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
