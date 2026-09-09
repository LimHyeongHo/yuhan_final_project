package com.Nbbang.backend.domain.admin.security.controller;

import com.Nbbang.backend.domain.admin.security.entity.SecuritySimulationMode;
import com.Nbbang.backend.domain.admin.security.dto.SimulationStartRequest;
import com.Nbbang.backend.domain.admin.security.service.SecuritySimulationService;
import com.Nbbang.backend.global.exception.CustomException;
import com.Nbbang.backend.global.exception.ErrorCode;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/security")
@RequiredArgsConstructor
public class SecuritySimulationController {

    private final SecuritySimulationService simulationService;

    @GetMapping("/simulator/products")
    public ResponseEntity<List<Map<String, Object>>> getSimulatorProducts() {
        return ResponseEntity.ok(simulationService.getProducts());
    }

    @GetMapping("/simulator/products/{productId}/preview")
    public ResponseEntity<Map<String, Object>> preview(@PathVariable Long productId) {
        return ResponseEntity.ok(simulationService.preview(productId));
    }

    @PostMapping("/simulations")
    public ResponseEntity<Map<String, Object>> start(
            @RequestBody(required = false) SimulationStartRequest request,
            HttpSession session) {
        return ResponseEntity.status(HttpStatus.CREATED).body(simulationService.start(
                requireAdministrator(session),
                parseMode(request == null ? null : request.mode()),
                request == null ? null : request.productId(),
                request == null ? null : request.newPrice(),
                request == null ? null : request.reason(),
                request == null ? null : request.idempotencyKey()));
    }

    @GetMapping("/simulations/{simulationId}")
    public ResponseEntity<Map<String, Object>> get(
            @PathVariable String simulationId,
            HttpSession session) {
        return ResponseEntity.ok(simulationService.get(requireAdministrator(session), simulationId));
    }

    @PostMapping("/simulations/{simulationId}/verify")
    public ResponseEntity<Map<String, Object>> verify(
            @PathVariable String simulationId,
            HttpSession session) {
        return ResponseEntity.ok(simulationService.verify(requireAdministrator(session), simulationId));
    }

    @PostMapping("/simulations/{simulationId}/restore")
    public ResponseEntity<Map<String, Object>> restore(
            @PathVariable String simulationId,
            HttpSession session) {
        return ResponseEntity.ok(simulationService.restore(requireAdministrator(session), simulationId));
    }

    private String requireAdministrator(HttpSession session) {
        String administratorId = session == null ? null : (String) session.getAttribute("userId");
        if (administratorId == null || administratorId.isBlank()) {
            throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
        }
        return administratorId;
    }

    private SecuritySimulationMode parseMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return SecuritySimulationMode.RANDOM;
        }
        try {
            return SecuritySimulationMode.valueOf(mode.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new CustomException(ErrorCode.VALIDATION_FAILED);
        }
    }
}
