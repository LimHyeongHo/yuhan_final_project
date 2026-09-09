package com.Nbbang.backend.domain.admin.security.service;

import com.Nbbang.backend.domain.admin.security.entity.SecuritySimulation;
import com.Nbbang.backend.domain.admin.security.entity.SecuritySimulationMode;
import com.Nbbang.backend.domain.admin.security.entity.SecuritySimulationStatus;
import com.Nbbang.backend.domain.admin.security.repository.SecuritySimulationRepository;
import com.Nbbang.backend.domain.log.service.SystemLogService;
import com.Nbbang.backend.domain.product.entity.BlockchainJobStatus;
import com.Nbbang.backend.domain.product.entity.Product;
import com.Nbbang.backend.domain.product.repository.ProductRepository;
import com.Nbbang.backend.domain.product.service.BlockchainService;
import com.Nbbang.backend.domain.product.service.ProductHashService;
import com.Nbbang.backend.global.exception.CustomException;
import com.Nbbang.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SecuritySimulationService {
    private static final BigDecimal MAX_TAMPER_PRICE = new BigDecimal("5000000");

    private static final BigDecimal RANDOM_TAMPERED_PRICE = new BigDecimal("999999");
    private static final Set<SecuritySimulationStatus> ACTIVE_STATUSES = Set.of(
            SecuritySimulationStatus.SNAPSHOT_SAVED,
            SecuritySimulationStatus.FORGED_DETECTED,
            SecuritySimulationStatus.RESTORED,
            SecuritySimulationStatus.VERIFICATION_UNAVAILABLE,
            SecuritySimulationStatus.HASH_MISMATCH,
            SecuritySimulationStatus.RECOVERY_REQUIRED);

    private final ProductRepository productRepository;
    private final SecuritySimulationRepository simulationRepository;
    private final ProductHashService productHashService;
    private final BlockchainService blockchainService;
    private final SystemLogService systemLogService;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getProducts() {
        List<Map<String, Object>> result = new ArrayList<>();
        productRepository.findAll().stream()
                .sorted((left, right) -> {
                    if (left.getCreatedAt() == null || right.getCreatedAt() == null) {
                        return Long.compare(right.getProductId(), left.getProductId());
                    }
                    return right.getCreatedAt().compareTo(left.getCreatedAt());
                })
                .forEach(product -> {
                    Map<String, Object> summary = productSummary(product);
                    var activeSimulation = simulationRepository
                            .findFirstByProductIdAndStatusInOrderByStartedAtDesc(
                                    product.getProductId(), ACTIVE_STATUSES);
                    summary.put("canSimulate", canSimulate(product) && activeSimulation.isEmpty());
                    activeSimulation.ifPresent(simulation -> {
                        summary.put("tampered", true);
                        summary.put("simulationId", simulation.getSimulationId());
                        summary.put("simulationStatus", simulation.getStatus());
                        summary.put("originalPrice", simulation.getOriginalPrice());
                        summary.put("tamperedPrice", simulation.getTamperedPrice());
                        summary.put("warning", "가격 데이터가 변경되어 해시 위변조가 감지되었습니다.");
                    });
                    result.add(summary);
                });
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> preview(Long productId) {
        Product product = findProduct(productId);
        BlockchainService.BlockchainReadResult read = blockchainService.readHash(productId);
        Map<String, Object> result = productSummary(product);
        result.put("dbHash", productHashService.calculateHash(product));
        result.put("onChainHash", read.hash());
        result.put("onChainCode", read.code());
        result.put("onChainMessage", read.message());
        result.put("hashMatches", read.success()
                && productHashService.matches(productHashService.calculateHash(product), read.hash()));
        result.put("canSimulate", canSimulate(product)
                && !simulationRepository.existsByProductIdAndStatusIn(productId, ACTIVE_STATUSES)
                && read.success()
                && productHashService.matches(productHashService.calculateHash(product), read.hash()));
        return result;
    }

    @Transactional
    public Map<String, Object> start(
            String administratorId,
            SecuritySimulationMode requestedMode,
            Long requestedProductId,
            BigDecimal requestedPrice,
            String reason,
            String idempotencyKey) {
        if (administratorId == null || administratorId.isBlank()) {
            throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
        }

        SecuritySimulationMode mode = requestedMode == null
                ? SecuritySimulationMode.RANDOM
                : requestedMode;
        String normalizedKey = normalize(idempotencyKey);
        if (normalizedKey != null) {
            var existing = simulationRepository
                    .findByAdministratorIdAndIdempotencyKey(administratorId, normalizedKey);
            if (existing.isPresent()) {
                return response(existing.get(), productRepository.findById(existing.get().getProductId()).orElse(null));
            }
        }

        BigDecimal tamperedPrice = mode == SecuritySimulationMode.RANDOM
                ? RANDOM_TAMPERED_PRICE
                : validateTargetedPrice(requestedPrice);

        Long productId = mode == SecuritySimulationMode.RANDOM
                ? selectRandomProductId()
                : requireProductId(requestedProductId);
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        ensureEligible(product);
        if (simulationRepository.existsByProductIdAndStatusIn(productId, ACTIVE_STATUSES)) {
            throw new CustomException(ErrorCode.ADMIN_SIMULATION_CONFLICT);
        }

        BlockchainService.BlockchainReadResult read = blockchainService.readHash(productId);
        if (!read.success() || read.hash() == null || read.hash().isBlank()) {
            throw blockchainError(read);
        }

        BigDecimal originalPrice = product.getPrice() == null ? BigDecimal.ZERO : product.getPrice();
        String originalDbHash = productHashService.calculateHash(product);
        if (!productHashService.matches(originalDbHash, read.hash())) {
            throw new CustomException(ErrorCode.ADMIN_SIMULATION_NOT_ELIGIBLE);
        }
        if (originalPrice.compareTo(tamperedPrice) == 0) {
            throw new CustomException(ErrorCode.VALIDATION_FAILED);
        }

        SecuritySimulation simulation = SecuritySimulation.create(
                mode,
                administratorId,
                productId,
                product.getTitle(),
                originalPrice,
                tamperedPrice,
                originalDbHash,
                read.hash(),
                product.getPriceVersion() == null ? 1 : product.getPriceVersion(),
                normalize(reason),
                normalizedKey);
        simulationRepository.saveAndFlush(simulation);

        product.setPrice(tamperedPrice);
        productRepository.save(product);
        simulation.markForgedDetected(productHashService.calculateHash(product));
        simulationRepository.save(simulation);
        systemLogService.log("SECURITY", "TAMPERED",
                "시뮬레이션 " + simulation.getSimulationId()
                        + " / 관리자 " + administratorId
                        + " / 상품 " + productId
                        + " / 가격 " + originalPrice + " -> " + tamperedPrice);
        return response(simulation, product);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> get(String administratorId, String simulationId) {
        SecuritySimulation simulation = findSimulation(simulationId);
        ensureAdministrator(administratorId, simulation);
        Product product = productRepository.findById(simulation.getProductId()).orElse(null);
        return response(simulation, product);
    }

    @Transactional
    public Map<String, Object> verify(String administratorId, String simulationId) {
        SecuritySimulation simulation = findSimulation(simulationId);
        ensureAdministrator(administratorId, simulation);
        Product product = productRepository.findByIdForUpdate(simulation.getProductId())
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        BlockchainService.BlockchainReadResult read = blockchainService.readHash(product.getProductId());
        if (!read.success() || read.hash() == null || read.hash().isBlank()) {
            simulation.markVerificationUnavailable(read.message() == null
                    ? "블록체인 원본을 확인할 수 없습니다."
                    : read.message());
            simulationRepository.save(simulation);
            return response(simulation, product);
        }

        String currentDbHash = productHashService.calculateHash(product);
        if (productHashService.matches(currentDbHash, read.hash())) {
            simulation.markRecoveryRequired("현재 DB 값은 블록체인 원본과 일치합니다. 정상 변경 여부를 확인하세요.");
        } else {
            simulation.markForgedDetected(currentDbHash);
        }
        simulationRepository.save(simulation);
        return response(simulation, product);
    }

    @Transactional
    public Map<String, Object> restore(String administratorId, String simulationId) {
        SecuritySimulation simulation = findSimulation(simulationId);
        ensureAdministrator(administratorId, simulation);
        if (simulation.getStatus() == SecuritySimulationStatus.VERIFIED) {
            return response(simulation, productRepository.findById(simulation.getProductId()).orElse(null));
        }

        Product product = productRepository.findByIdForUpdate(simulation.getProductId())
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
        BigDecimal currentPrice = product.getPrice() == null ? BigDecimal.ZERO : product.getPrice();

        if (currentPrice.compareTo(simulation.getTamperedPrice()) != 0
                && currentPrice.compareTo(simulation.getOriginalPrice()) != 0) {
            simulation.markRecoveryRequired("시뮬레이션 이후 정상 가격 변경이 감지되어 자동 복구를 중단했습니다.");
            simulationRepository.save(simulation);
            throw new CustomException(ErrorCode.ADMIN_SIMULATION_CONFLICT);
        }

        product.setPrice(simulation.getOriginalPrice());
        productRepository.save(product);
        simulation.markRestored();
        simulationRepository.save(simulation);

        BlockchainService.BlockchainReadResult read = blockchainService.readHash(product.getProductId());
        if (!read.success() || read.hash() == null || read.hash().isBlank()) {
            simulation.markVerificationUnavailable(read.message() == null
                    ? "DB 복구는 완료했지만 블록체인 재검증을 할 수 없습니다."
                    : read.message());
        } else {
            String restoredHash = productHashService.calculateHash(product);
            if (productHashService.matches(restoredHash, read.hash())) {
                simulation.markVerified(read.hash());
            } else {
                simulation.markHashMismatch(read.hash());
            }
        }
        simulationRepository.save(simulation);
        systemLogService.log("SECURITY",
                simulation.getStatus() == SecuritySimulationStatus.VERIFIED ? "SUCCESS" : "FAILED",
                "시뮬레이션 복구 " + simulation.getSimulationId()
                        + " / 관리자 " + administratorId
                        + " / 상품 " + simulation.getProductId()
                        + " / 결과 " + simulation.getStatus());
        return response(simulation, product);
    }

    private Long selectRandomProductId() {
        List<Product> candidates = new ArrayList<>(productRepository.findAll());
        Collections.shuffle(candidates);
        for (Product product : candidates) {
            if (!canSimulate(product)
                    || simulationRepository.existsByProductIdAndStatusIn(product.getProductId(), ACTIVE_STATUSES)) {
                continue;
            }
            BlockchainService.BlockchainReadResult read = blockchainService.readHash(product.getProductId());
            if (read.success() && productHashService.matches(
                    productHashService.calculateHash(product), read.hash())) {
                return product.getProductId();
            }
        }
        throw new CustomException(ErrorCode.ADMIN_SIMULATION_NOT_ELIGIBLE);
    }

    private Product findProduct(Long productId) {
        if (productId == null) {
            throw new CustomException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    private SecuritySimulation findSimulation(String simulationId) {
        if (simulationId == null || simulationId.isBlank()) {
            throw new CustomException(ErrorCode.ADMIN_SIMULATION_NOT_FOUND);
        }
        return simulationRepository.findById(simulationId)
                .orElseThrow(() -> new CustomException(ErrorCode.ADMIN_SIMULATION_NOT_FOUND));
    }

    private void ensureAdministrator(String administratorId, SecuritySimulation simulation) {
        if (administratorId == null || !administratorId.equals(simulation.getAdministratorId())) {
            throw new CustomException(ErrorCode.AUTH_ACCESS_DENIED);
        }
    }

    private void ensureEligible(Product product) {
        if (!canSimulate(product)) {
            throw new CustomException(ErrorCode.ADMIN_SIMULATION_NOT_ELIGIBLE);
        }
    }

    private boolean canSimulate(Product product) {
        return product != null
                && product.getBlockchainStatus() == BlockchainJobStatus.CONFIRMED
                && !"SELLER_WITHDRAWN".equals(product.getStatus());
    }

    private BigDecimal validateTargetedPrice(BigDecimal price) {
        if (price == null || price.signum() <= 0 || price.scale() > 2
                || price.compareTo(MAX_TAMPER_PRICE) > 0) {
            throw new CustomException(ErrorCode.VALIDATION_FAILED);
        }
        return price.stripTrailingZeros();
    }

    private Long requireProductId(Long productId) {
        if (productId == null) {
            throw new CustomException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return productId;
    }

    private CustomException blockchainError(BlockchainService.BlockchainReadResult read) {
        if (read != null && "UNAVAILABLE".equals(read.code())) {
            return new CustomException(ErrorCode.ADMIN_SIMULATION_BLOCKCHAIN_UNAVAILABLE);
        }
        return new CustomException(ErrorCode.ADMIN_SIMULATION_NOT_ELIGIBLE);
    }

    private Map<String, Object> productSummary(Product product) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", product.getProductId());
        result.put("productId", product.getProductId());
        result.put("title", product.getTitle());
        result.put("category", product.getCategory() == null ? "기타" : product.getCategory());
        result.put("price", product.getPrice());
        result.put("seller", product.getSellerEmail() == null ? "알 수 없음" : product.getSellerEmail());
        result.put("status", product.getStatus());
        result.put("blockchainStatus", product.getBlockchainStatus());
        result.put("txHash", product.getTxHash());
        result.put("canSimulate", canSimulate(product));
        return result;
    }

    private Map<String, Object> response(SecuritySimulation simulation, Product product) {
        Map<String, Object> result = new HashMap<>();
        result.put("simulationId", simulation.getSimulationId());
        result.put("mode", simulation.getMode());
        result.put("status", simulation.getStatus());
        result.put("message", simulation.getMessage());
        result.put("productId", simulation.getProductId());
        result.put("productTitle", simulation.getProductTitle());
        result.put("originalPrice", simulation.getOriginalPrice());
        result.put("tamperedPrice", simulation.getTamperedPrice());
        result.put("newPrice", simulation.getTamperedPrice());
        result.put("originalDbHash", simulation.getOriginalDbHash());
        result.put("activeOnChainHash", simulation.getActiveOnChainHash());
        result.put("tamperedDbHash", simulation.getTamperedDbHash());
        result.put("lastObservedOnChainHash", simulation.getLastObservedOnChainHash());
        result.put("startedAt", simulation.getStartedAt());
        result.put("restoredAt", simulation.getRestoredAt());
        result.put("finishedAt", simulation.getFinishedAt());
        if (product != null) {
            result.put("currentPrice", product.getPrice());
            result.put("currentDbHash", productHashService.calculateHash(product));
            result.put("category", product.getCategory());
        }
        return result;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
