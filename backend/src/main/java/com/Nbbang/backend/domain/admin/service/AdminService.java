package com.Nbbang.backend.domain.admin.service;

import com.Nbbang.backend.domain.auth.entity.UserAccount;
import com.Nbbang.backend.domain.auth.repository.UserAccountRepository;
import com.Nbbang.backend.domain.notification.entity.Notification;
import com.Nbbang.backend.domain.notification.repository.NotificationRepository;
import com.Nbbang.backend.domain.product.entity.Product;
import com.Nbbang.backend.domain.product.repository.ProductRepository;
import com.Nbbang.backend.domain.product.service.BlockchainService;
import com.Nbbang.backend.domain.product.service.ProductHashService;
import com.Nbbang.backend.global.exception.CustomException;
import com.Nbbang.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserAccountRepository userAccountRepository;
    private final ProductRepository productRepository;
    private final NotificationRepository notificationRepository;
    private final BlockchainService blockchainService;
    private final ProductHashService productHashService;
    private final Map<Long, java.math.BigDecimal> hackPriceSnapshots = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        long totalUsers = userAccountRepository.count();
        long newUsersToday = userAccountRepository.countByCreatedAtAfter(LocalDate.now().atStartOfDay());
        long activeSellers = userAccountRepository.countByRole("ROLE_SELLER");
        long totalProducts = productRepository.count();
        
        stats.put("totalUsers", totalUsers);
        stats.put("newUsersToday", newUsersToday);
        stats.put("activeSellers", activeSellers);
        stats.put("totalProducts", totalProducts);
        
        return stats;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPendingSellers() {
        return userAccountRepository.findByRoleOrderByCreatedAtDesc("ROLE_SELLER_PENDING").stream()
            .map(user -> {
                Map<String, Object> map = new HashMap<>();
                map.put("email", user.getEmail());
                map.put("nickname", user.getNickname());
                map.put("createdAt", user.getCreatedAt().toLocalDate().toString());
                return map;
            })
            .collect(Collectors.toList());
    }

    @Transactional
    public void grantSellerRole(String email) {
        UserAccount user = userAccountRepository.findById(email)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        
        if (!"ROLE_SELLER_PENDING".equals(user.getRole())) {
            throw new CustomException(ErrorCode.VALIDATION_FAILED); // 승인 대기 상태가 아님
        }
        
        user.setRole("ROLE_SELLER");
    }

    @Transactional(readOnly = true)
    public Map<String, List<Long>> getWeeklyStats() {
        List<Long> signupData = new java.util.ArrayList<>();
        List<Long> sellerData = new java.util.ArrayList<>();
        
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            java.time.LocalDateTime start = date.atStartOfDay();
            java.time.LocalDateTime end = date.plusDays(1).atStartOfDay();
            
            signupData.add(userAccountRepository.countByCreatedAtBetween(start, end));
            sellerData.add(userAccountRepository.countByRoleAndCreatedAtBetween("ROLE_SELLER", start, end));
        }
        
        Map<String, List<Long>> result = new HashMap<>();
        result.put("signupData", signupData);
        result.put("sellerData", sellerData);
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRecentProducts() {
        return productRepository.findTop5ByOrderByCreatedAtDesc().stream()
            .map(product -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", product.getProductId());
                map.put("title", product.getTitle());
                map.put("category", product.getCategory());
                map.put("price", product.getPrice());
                map.put("targetCount", product.getTargetCount());
                String currentStatus = product.getStatus();
                if (product.getCurrentCount() != null && product.getTargetCount() != null && product.getCurrentCount() >= product.getTargetCount()) {
                    if ("OPEN".equals(currentStatus)) {
                        currentStatus = "CLOSED_SUCCESS";
                    }
                }
                boolean isSuspicious = product.getPrice() != null && product.getPrice().intValue() > 500000;
                if (isSuspicious) {
                    currentStatus = "TAMPERED";
                }
                map.put("status", currentStatus);
                map.put("date", product.getCreatedAt().toLocalDate().toString());
                return map;
            })
            .collect(Collectors.toList());
    }

    // 신규 가입자 목록 조회 (n일 이내)
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getUsersStatsList(int days) {
        LocalDate startDate = LocalDate.now().minusDays(days);
        java.time.LocalDateTime startDateTime = startDate.atStartOfDay();

        return userAccountRepository.findAll().stream()
            .filter(user -> user.getCreatedAt().isAfter(startDateTime))
            .sorted(java.util.Comparator.comparing(UserAccount::getCreatedAt).reversed())
            .map(user -> {
                Map<String, Object> map = new HashMap<>();
                String email = user.getEmail();
                int atIndex = email.indexOf("@");
                String maskedEmail = email;
                if (atIndex > 4) {
                    maskedEmail = email.substring(0, 4) + "***" + email.substring(atIndex);
                } else if (atIndex > 0) {
                    maskedEmail = email.substring(0, atIndex) + "***" + email.substring(atIndex);
                } else if (email.length() > 4) {
                    maskedEmail = email.substring(0, 4) + "***";
                }
                
                map.put("email", maskedEmail);
                map.put("nickname", user.getNickname());
                map.put("createdAt", user.getCreatedAt().toLocalDate().toString());
                return map;
            })
            .collect(Collectors.toList());
    }

    // 판매자 통계 목록 조회
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getSellersStatsList() {
        return userAccountRepository.findByRoleOrderByCreatedAtDesc("ROLE_SELLER").stream()
            .map(seller -> {
                Map<String, Object> map = new HashMap<>();
                String email = seller.getEmail();
                int atIndex = email.indexOf("@");
                String maskedEmail = email;
                if (atIndex > 4) {
                    maskedEmail = email.substring(0, 4) + "***" + email.substring(atIndex);
                } else if (atIndex > 0) {
                    maskedEmail = email.substring(0, atIndex) + "***" + email.substring(atIndex);
                } else if (email.length() > 4) {
                    maskedEmail = email.substring(0, 4) + "***";
                }
                
                map.put("email", maskedEmail);
                map.put("nickname", seller.getNickname());
                
                // 판매자 상품들
                List<Product> products = productRepository.findBySellerEmailOrderByCreatedAtDesc(seller.getEmail());
                long productCount = products.size();
                long totalRevenue = products.stream()
                        .mapToLong(p -> p.getPrice().longValue() * p.getCurrentCount())
                        .sum();
                        
                map.put("productCount", productCount);
                map.put("totalRevenue", totalRevenue);
                return map;
            })
            .collect(Collectors.toList());
    }

    // 어드민용 전체 상품 리스트 조회
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllProductsForAdmin() {
        return productRepository.findAll().stream()
            .sorted(java.util.Comparator.comparing(Product::getCreatedAt).reversed())
            .map(product -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", product.getProductId());
                map.put("title", product.getTitle());
                map.put("category", product.getCategory() != null ? product.getCategory() : "기타");
                map.put("price", product.getPrice());
                map.put("targetCount", product.getTargetCount());
                map.put("currentCount", product.getCurrentCount());
                String currentStatus = product.getStatus();
                if (product.getCurrentCount() != null && product.getTargetCount() != null && product.getCurrentCount() >= product.getTargetCount()) {
                    if ("OPEN".equals(currentStatus)) {
                        currentStatus = "CLOSED_SUCCESS";
                        // DB에도 동기화 업데이트 (선택 사항이지만 일치시키는 것이 좋음)
                        product.setStatus("CLOSED_SUCCESS");
                    }
                }
                
                map.put("seller", product.getSellerEmail() != null ? product.getSellerEmail() : "알 수 없음");
                map.put("date", product.getCreatedAt().toLocalDate().toString());
                
                int ratio = 0;
                if (product.getTargetCount() != null && product.getTargetCount() > 0) {
                    ratio = (int) (((double) product.getCurrentCount() / product.getTargetCount()) * 100);
                }
                map.put("ratio", ratio);
                
                boolean isSuspicious = product.getPrice() != null && product.getPrice().intValue() > 500000;
                if (isSuspicious) {
                    currentStatus = "TAMPERED";
                }
                
                map.put("status", currentStatus);
                map.put("suspicious", isSuspicious);
                
                return map;
            })
            .collect(Collectors.toList());
    }

    // 어드민 전용 상품 강제 삭제
    @Transactional
    public void deleteProductByAdmin(Long productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new CustomException(ErrorCode.VALIDATION_FAILED));
        productRepository.delete(product);
    }

    // 어드민 전용 상품 거절 (사유 포함)
    @Transactional
    public void rejectProductByAdmin(Long productId, String reason) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new CustomException(ErrorCode.VALIDATION_FAILED));
        
        if (product.getSellerEmail() != null) {
            String shortTitle = product.getTitle();
            if (shortTitle != null && shortTitle.contains("-")) {
                shortTitle = shortTitle.split("-")[0].trim();
            }
            String message = String.format("등록하신 상품(ID: %d, 제목: %s)이 관리자에 의해 거절/삭제되었습니다. \n사유: %s", 
                product.getProductId(), shortTitle, reason);
            Notification notif = new Notification(product.getSellerEmail(), message);
            notificationRepository.save(notif);
        }
        
        productRepository.delete(product);
    }

    // [신규] 보안 검증 시뮬레이터: 해킹 시뮬레이션
    @Transactional
    public java.util.Map<String, Object> simulateHack() {
        // 온체인 기록이 존재하고 현재 DB 해시와 일치하는 상품 중 무작위로 1개를 선택
        java.util.List<Product> validProducts = productRepository.findAll().stream()
                .filter(p -> {
                    BlockchainService.BlockchainReadResult readResult =
                            blockchainService.readHash(p.getProductId());
                    return readResult.success()
                            && readResult.hash() != null
                            && !readResult.hash().isEmpty()
                            && productHashService.matches(
                                    productHashService.calculateHash(p), readResult.hash());
                })
                .collect(Collectors.toList());
                
        if (validProducts.isEmpty()) {
            throw new CustomException(ErrorCode.VALIDATION_FAILED);
        }
        
        Product product = validProducts.get(new java.util.Random().nextInt(validProducts.size()));

        java.math.BigDecimal currentPrice = product.getPrice() != null ? product.getPrice() : java.math.BigDecimal.ZERO;
        String isbn = product.getIsbn();
        String originalHash = productHashService.calculateHash(product);

        // 복구 시 정가가 아닌 변조 직전의 실제 공동구매 가격을 사용한다.
        hackPriceSnapshots.putIfAbsent(product.getProductId(), currentPrice);

        // 고의로 가격을 999,999원으로 변조하여 DB에 저장 (블록체인 우회)
        java.math.BigDecimal newPrice = new java.math.BigDecimal("999999");
        product.setPrice(newPrice);
        productRepository.save(product);

        // 변조된 해시 계산
        String newHash = productHashService.calculateHash(product.getProductId(), isbn, newPrice);

        java.util.Map<String, Object> result = new HashMap<>();
        result.put("productId", product.getProductId());
        result.put("originalPrice", currentPrice);
        result.put("newPrice", newPrice);
        result.put("originalHash", originalHash);
        result.put("newHash", newHash);

        return result;
    }

    // [신규] 보안 검증 시뮬레이터: 정상 복구
    @Transactional
    public void restoreHack(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.VALIDATION_FAILED));
        
        java.math.BigDecimal origPrice = hackPriceSnapshots.get(productId);
        if (origPrice == null) {
            throw new CustomException(ErrorCode.VALIDATION_FAILED);
        }

        product.setPrice(origPrice);
        productRepository.save(product);
        hackPriceSnapshots.remove(productId, origPrice);
    }

    // [신규] 레거시 데이터 블록체인 마이그레이션
    public java.util.Map<String, Object> migrateLegacyData() {
        return migrateLegacyData(progress -> { });
    }

    public java.util.Map<String, Object> migrateLegacyData(Consumer<MigrationProgress> progressListener) {
        java.util.Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> items = new ArrayList<>();
        List<Product> products = productRepository.findAll();
        int totalCount = products.size();
        int processedCount = 0;
        int confirmedCount = 0;
        int failedCount = 0;
        int alreadySyncedCount = 0;
        int remediatedCount = 0;
        int mismatchCount = 0;
        int pendingCount = 0;
        boolean stoppedEarly = false;

        notifyMigrationProgress(progressListener, totalCount, processedCount,
                confirmedCount, failedCount, alreadySyncedCount,
                remediatedCount, mismatchCount, null);

        for (Product product : products) {
            notifyMigrationProgress(progressListener, totalCount, processedCount,
                    confirmedCount, failedCount, alreadySyncedCount,
                    remediatedCount, mismatchCount, product.getProductId());

            String expectedHash = productHashService.calculateHash(product);
            BlockchainService.BlockchainReadResult readResult =
                    blockchainService.readHash(product.getProductId());

            if (!readResult.success()) {
                failedCount++;
                items.add(migrationItem(product.getProductId(), "RPC_ERROR",
                        product.getTxHash(), readResult.message()));
                processedCount++;
                notifyMigrationProgress(progressListener, totalCount, processedCount,
                        confirmedCount, failedCount, alreadySyncedCount,
                        remediatedCount, mismatchCount, product.getProductId());
                continue;
            }

            String hashOnChain = readResult.hash();

            if (hashOnChain != null && !hashOnChain.isEmpty()) {
                if (productHashService.matches(expectedHash, hashOnChain)) {
                    alreadySyncedCount++;
                    processedCount++;
                    notifyMigrationProgress(progressListener, totalCount, processedCount,
                            confirmedCount, failedCount, alreadySyncedCount,
                            remediatedCount, mismatchCount, product.getProductId());
                    continue;
                }

                String legacyHash = productHashService.calculateLegacyMigrationHash(product);
                if (productHashService.matches(legacyHash, hashOnChain)) {
                    BlockchainService.BlockchainWriteResult remediationResult =
                            blockchainService.recordHashAndConfirm(product.getProductId(), expectedHash);
                    if (remediationResult.success()) {
                        confirmedCount++;
                        remediatedCount++;
                        items.add(migrationItem(product.getProductId(), "REMEDIATED",
                                remediationResult.txHash(), "구형 해시 규격을 현재 규격으로 재기록했습니다."));
                    } else if ("TIMEOUT".equals(remediationResult.code())) {
                        pendingCount++;
                        stoppedEarly = true;
                        items.add(migrationItem(product.getProductId(), "PENDING",
                                remediationResult.txHash(), remediationResult.message()));
                    } else {
                        failedCount++;
                        if ("TXPOOL_FULL".equals(remediationResult.code())) {
                            stoppedEarly = true;
                        }
                        items.add(migrationItem(product.getProductId(), "FAILED",
                                remediationResult.txHash(), remediationResult.message()));
                    }
                } else {
                    mismatchCount++;
                    items.add(migrationItem(product.getProductId(), "HASH_MISMATCH",
                            product.getTxHash(), "기존 온체인 해시와 현재 DB 해시가 달라 자동 덮어쓰기를 중단했습니다."));
                }
                processedCount++;
                notifyMigrationProgress(progressListener, totalCount, processedCount,
                        confirmedCount, failedCount, alreadySyncedCount,
                        remediatedCount, mismatchCount, product.getProductId());
                if (stoppedEarly) {
                    break;
                }
                continue;
            }

            BlockchainService.BlockchainWriteResult writeResult;
            boolean remediated = false;
            if (product.getTxHash() != null && !product.getTxHash().isBlank()) {
                writeResult = blockchainService.confirmExistingTransaction(
                        product.getProductId(), expectedHash, product.getTxHash());
            } else {
                writeResult = blockchainService.recordHashAndConfirm(product.getProductId(), expectedHash);
            }

            if (writeResult.success()) {
                confirmedCount++;
                if (remediated) {
                    remediatedCount++;
                }
                items.add(migrationItem(product.getProductId(), remediated ? "REMEDIATED" : "CONFIRMED",
                        writeResult.txHash(), remediated
                                ? "구형 해시 규격을 현재 규격으로 재기록했습니다."
                                : writeResult.message()));
            } else if ("PENDING".equals(writeResult.code()) || "TIMEOUT".equals(writeResult.code())) {
                pendingCount++;
                stoppedEarly = true;
                items.add(migrationItem(product.getProductId(), "PENDING",
                        writeResult.txHash(), writeResult.message()));
            } else {
                failedCount++;
                items.add(migrationItem(product.getProductId(), "FAILED",
                        writeResult.txHash(), writeResult.message()));
            }

            processedCount++;
            notifyMigrationProgress(progressListener, totalCount, processedCount,
                    confirmedCount, failedCount, alreadySyncedCount,
                    remediatedCount, mismatchCount, product.getProductId());

            if ("PENDING".equals(writeResult.code())
                    || "TIMEOUT".equals(writeResult.code())
                    || "TXPOOL_FULL".equals(writeResult.code())) {
                stoppedEarly = true;
                break;
            }
        }

        String status = failedCount == 0 && mismatchCount == 0 && pendingCount == 0 && !stoppedEarly
                ? "SUCCESS"
                : (confirmedCount > 0 || alreadySyncedCount > 0 || pendingCount > 0
                        ? "PARTIAL_SUCCESS" : "FAILED");

        result.put("status", status);
        result.put("message", "블록체인 확정 확인이 완료되었습니다.");
        result.put("totalCount", totalCount);
        result.put("processedCount", processedCount);
        result.put("count", confirmedCount);
        result.put("confirmedCount", confirmedCount);
        result.put("failedCount", failedCount);
        result.put("alreadySyncedCount", alreadySyncedCount);
        result.put("remediatedCount", remediatedCount);
        result.put("mismatchCount", mismatchCount);
        result.put("pendingCount", pendingCount);
        result.put("stoppedEarly", stoppedEarly);
        result.put("remainingCount", Math.max(0, totalCount - processedCount));
        result.put("items", items);
        return result;
    }

    private void notifyMigrationProgress(
            Consumer<MigrationProgress> listener,
            int totalCount,
            int processedCount,
            int confirmedCount,
            int failedCount,
            int alreadySyncedCount,
            int remediatedCount,
            int mismatchCount,
            Long currentProductId) {
        listener.accept(new MigrationProgress(
                totalCount,
                processedCount,
                confirmedCount,
                failedCount,
                alreadySyncedCount,
                remediatedCount,
                mismatchCount,
                currentProductId));
    }

    private Map<String, Object> migrationItem(
            Long productId, String status, String txHash, String message) {
        Map<String, Object> item = new HashMap<>();
        item.put("productId", productId);
        item.put("status", status);
        item.put("txHash", txHash);
        item.put("message", message);
        return item;
    }

    public record MigrationProgress(
            int totalCount,
            int processedCount,
            int confirmedCount,
            int failedCount,
            int alreadySyncedCount,
            int remediatedCount,
            int mismatchCount,
            Long currentProductId) {
    }
}
