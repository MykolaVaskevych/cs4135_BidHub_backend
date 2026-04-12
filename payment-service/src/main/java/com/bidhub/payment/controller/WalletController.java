package com.bidhub.payment.controller;

import com.bidhub.payment.dto.*;
import com.bidhub.payment.service.WalletService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/wallet")
    public ResponseEntity<WalletResponse> getWallet(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(walletService.getOrCreateWallet(UUID.fromString(userId)));
    }

    @PostMapping("/wallet/top-up")
    public ResponseEntity<WalletResponse> topUp(
            @RequestHeader("X-User-Id") String userId, @Valid @RequestBody TopUpRequest request) {
        return ResponseEntity.ok(walletService.topUp(UUID.fromString(userId), request));
    }

    @PostMapping("/wallet/deduct")
    public ResponseEntity<WalletResponse> deduct(
            @RequestHeader("X-User-Id") String userId, @Valid @RequestBody DeductRequest request) {
        return ResponseEntity.ok(walletService.deduct(UUID.fromString(userId), request));
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionResponse>> getTransactions(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(walletService.getTransactionHistory(UUID.fromString(userId)));
    }
}
