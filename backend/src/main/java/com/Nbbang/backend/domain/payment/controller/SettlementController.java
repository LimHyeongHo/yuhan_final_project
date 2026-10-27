package com.Nbbang.backend.domain.payment.controller;

import com.Nbbang.backend.domain.payment.dto.*;
import com.Nbbang.backend.domain.payment.service.SettlementService;
import com.Nbbang.backend.global.exception.CustomException;
import com.Nbbang.backend.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 판매자 정산 계좌/출금 신청 API → SettlementService (송금은 관리자 수동 승인)
@RestController
@RequestMapping("/api/settlement")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class SettlementController {

    private final SettlementService settlementService;

    @GetMapping("/account")
    public ResponseEntity<SettlementAccountResponse> getAccount(HttpServletRequest httpRequest) {
        String sellerEmail = requireSellerEmail(httpRequest);
        return ResponseEntity.ok(settlementService.getAccount(sellerEmail));
    }

    @PostMapping("/account")
    public ResponseEntity<SettlementAccountResponse> registerAccount(@RequestBody SettlementAccountRequest request,
                                                                        HttpServletRequest httpRequest) {
        String sellerEmail = requireSellerEmail(httpRequest);
        return ResponseEntity.ok(settlementService.registerAccount(sellerEmail, request));
    }

    @GetMapping("/withdrawable")
    public ResponseEntity<WithdrawableAmountResponse> getWithdrawableAmount(HttpServletRequest httpRequest) {
        String sellerEmail = requireSellerEmail(httpRequest);
        return ResponseEntity.ok(settlementService.getWithdrawableAmount(sellerEmail));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<Void> requestWithdrawal(@RequestBody WithdrawalRequestDto request,
                                                     HttpServletRequest httpRequest) {
        String sellerEmail = requireSellerEmail(httpRequest);
        settlementService.requestWithdrawal(sellerEmail, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/history")
    public ResponseEntity<List<WithdrawalHistoryResponse>> getHistory(HttpServletRequest httpRequest) {
        String sellerEmail = requireSellerEmail(httpRequest);
        return ResponseEntity.ok(settlementService.getHistory(sellerEmail));
    }

    private String requireSellerEmail(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
        }
        if (!"ROLE_SELLER".equals(session.getAttribute("role"))) {
            throw new CustomException(ErrorCode.AUTH_ACCESS_DENIED);
        }
        return (String) session.getAttribute("userId");
    }
}
