package com.Nbbang.backend.domain.payment.service;

import com.Nbbang.backend.domain.payment.dto.*;
import com.Nbbang.backend.domain.payment.entity.SettlementAccount;
import com.Nbbang.backend.domain.payment.entity.WithdrawalRequest;
import com.Nbbang.backend.domain.payment.repository.PaymentRepository;
import com.Nbbang.backend.domain.payment.repository.SettlementAccountRepository;
import com.Nbbang.backend.domain.payment.repository.WithdrawalRequestRepository;
import com.Nbbang.backend.domain.product.entity.Product;
import com.Nbbang.backend.domain.product.repository.ProductRepository;
import com.Nbbang.backend.global.exception.CustomException;
import com.Nbbang.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private static final List<String> RESERVED_OR_PAID_STATUSES = List.of("REQUESTED", "COMPLETED");

    private final SettlementAccountRepository settlementAccountRepository;
    private final WithdrawalRequestRepository withdrawalRequestRepository;
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;

    public SettlementAccountResponse getAccount(String sellerEmail) {
        SettlementAccount account = settlementAccountRepository.findBySellerEmail(sellerEmail).orElse(null);
        if (account == null) {
            return null;
        }
        return new SettlementAccountResponse(account.getBankName(), account.getAccountNumber(), account.getAccountHolder());
    }

    @Transactional
    public SettlementAccountResponse registerAccount(String sellerEmail, SettlementAccountRequest request) {
        if (request.getBankName() == null || request.getBankName().isBlank()
                || request.getAccountNumber() == null || request.getAccountNumber().isBlank()
                || request.getAccountHolder() == null || request.getAccountHolder().isBlank()) {
            throw new CustomException(ErrorCode.VALIDATION_FAILED);
        }

        SettlementAccount account = settlementAccountRepository.findBySellerEmail(sellerEmail)
                .orElseGet(SettlementAccount::new);
        account.setSellerEmail(sellerEmail);
        account.setBankName(request.getBankName());
        account.setAccountNumber(request.getAccountNumber());
        account.setAccountHolder(request.getAccountHolder());

        try {
            settlementAccountRepository.saveAndFlush(account);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.MYPAGE_SETTLEMENT_ACCOUNT_UPDATE_FAILED);
        }

        return new SettlementAccountResponse(account.getBankName(), account.getAccountNumber(), account.getAccountHolder());
    }

    public WithdrawableAmountResponse getWithdrawableAmount(String sellerEmail) {
        return new WithdrawableAmountResponse(calculateWithdrawableAmount(sellerEmail));
    }

    private long calculateWithdrawableAmount(String sellerEmail) {
        List<Long> closedSuccessProductIds = productRepository.findBySellerEmailOrderByCreatedAtDesc(sellerEmail)
                .stream()
                .filter(p -> "CLOSED_SUCCESS".equals(p.getStatus()))
                .map(Product::getProductId)
                .toList();

        long totalEarned = 0L;
        if (!closedSuccessProductIds.isEmpty()) {
            totalEarned = paymentRepository.findByProductIdInAndStatus(closedSuccessProductIds, "DONE")
                    .stream()
                    .mapToLong(payment -> payment.getAmount() == null ? 0L : payment.getAmount())
                    .sum();
        }

        long reservedOrPaid = withdrawalRequestRepository
                .findBySellerEmailAndStatusIn(sellerEmail, RESERVED_OR_PAID_STATUSES)
                .stream()
                .mapToLong(WithdrawalRequest::getAmount)
                .sum();

        return Math.max(0L, totalEarned - reservedOrPaid);
    }

    @Transactional
    public void requestWithdrawal(String sellerEmail, WithdrawalRequestDto request) {
        if (request.getAmount() == null || request.getAmount() <= 0) {
            throw new CustomException(ErrorCode.VALIDATION_FAILED);
        }

        SettlementAccount account = settlementAccountRepository.findBySellerEmail(sellerEmail)
                .orElseThrow(() -> new CustomException(ErrorCode.MYPAGE_SETTLEMENT_NO_ACCOUNT));

        long withdrawable = calculateWithdrawableAmount(sellerEmail);
        if (request.getAmount() > withdrawable) {
            throw new CustomException(ErrorCode.VALIDATION_FAILED);
        }

        WithdrawalRequest withdrawalRequest = new WithdrawalRequest();
        withdrawalRequest.setSellerEmail(sellerEmail);
        withdrawalRequest.setAmount(request.getAmount());
        withdrawalRequest.setBankName(account.getBankName());
        withdrawalRequest.setAccountNumber(account.getAccountNumber());
        withdrawalRequest.setAccountHolder(account.getAccountHolder());

        try {
            withdrawalRequestRepository.saveAndFlush(withdrawalRequest);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.MYPAGE_SETTLEMENT_WITHDRAW_FAILED);
        }
    }

    public List<WithdrawalHistoryResponse> getHistory(String sellerEmail) {
        return withdrawalRequestRepository.findBySellerEmailOrderByRequestedAtDesc(sellerEmail)
                .stream()
                .map(w -> new WithdrawalHistoryResponse(w.getId(), w.getAmount(), w.getStatus(), w.getRequestedAt(), w.getProcessedAt()))
                .toList();
    }

    // ========== 관리자 승인 (실제 송금 자동화 없음, 수동 확인 후 상태만 변경) ==========

    public List<AdminWithdrawalResponse> getPendingWithdrawals() {
        return withdrawalRequestRepository.findByStatusOrderByRequestedAtAsc("REQUESTED")
                .stream()
                .map(w -> new AdminWithdrawalResponse(w.getId(), w.getSellerEmail(), w.getAmount(),
                        w.getBankName(), w.getAccountNumber(), w.getAccountHolder(), w.getStatus(), w.getRequestedAt()))
                .toList();
    }

    @Transactional
    public void approveWithdrawal(Long id) {
        WithdrawalRequest request = withdrawalRequestRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.ADMIN_TRANSACTION_APPROVE_FAILED));

        if (!"REQUESTED".equals(request.getStatus())) {
            throw new CustomException(ErrorCode.ADMIN_TRANSACTION_ALREADY_PROCESSED);
        }

        request.setStatus("COMPLETED");
        request.setProcessedAt(LocalDateTime.now());
    }

    @Transactional
    public void rejectWithdrawal(Long id, String reason) {
        WithdrawalRequest request = withdrawalRequestRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.ADMIN_TRANSACTION_REJECT_FAILED));

        if (!"REQUESTED".equals(request.getStatus())) {
            throw new CustomException(ErrorCode.ADMIN_TRANSACTION_ALREADY_PROCESSED);
        }

        request.setStatus("REJECTED");
        request.setRejectReason(reason);
        request.setProcessedAt(LocalDateTime.now());
    }
}
