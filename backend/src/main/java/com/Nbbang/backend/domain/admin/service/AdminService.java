package com.Nbbang.backend.domain.admin.service;

import com.Nbbang.backend.domain.auth.entity.UserAccount;
import com.Nbbang.backend.domain.auth.repository.UserAccountRepository;
import com.Nbbang.backend.domain.log.service.SystemLogService;
import com.Nbbang.backend.domain.notification.entity.Notification;
import com.Nbbang.backend.domain.notification.repository.NotificationRepository;
import com.Nbbang.backend.domain.product.entity.BlockchainJobStatus;
import com.Nbbang.backend.domain.product.entity.Product;
import com.Nbbang.backend.domain.product.repository.ProductRepository;
import com.Nbbang.backend.domain.product.service.BlockchainService;
import com.Nbbang.backend.domain.product.service.ProductHashService;
import com.Nbbang.backend.global.exception.CustomException;
import com.Nbbang.backend.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final UserAccountRepository userAccountRepository;
    private final ProductRepository productRepository;
    private final NotificationRepository notificationRepository;
    private final BlockchainService blockchainService;
    private final ProductHashService productHashService;
    private final SystemLogService systemLogService;

    @Autowired
    public AdminService(
            UserAccountRepository userAccountRepository,
            ProductRepository productRepository,
            NotificationRepository notificationRepository,
            BlockchainService blockchainService,
            ProductHashService productHashService,
            SystemLogService systemLogService) {
        this.userAccountRepository = userAccountRepository;
        this.productRepository = productRepository;
        this.notificationRepository = notificationRepository;
        this.blockchainService = blockchainService;
        this.productHashService = productHashService;
        this.systemLogService = systemLogService;
    }

    // 기존 단위 테스트와의 호환용 생성자. 운영 환경에서는 위의 전체 생성자를 사용한다.
    AdminService(
            UserAccountRepository userAccountRepository,
            ProductRepository productRepository,
            NotificationRepository notificationRepository,
            BlockchainService blockchainService,
            ProductHashService productHashService) {
        this(userAccountRepository, productRepository, notificationRepository,
                blockchainService, productHashService, null);
    }

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
    public void grantSellerRole(String email, String adminEmail) {
        UserAccount user = userAccountRepository.findById(email)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        if (!"ROLE_SELLER_PENDING".equals(user.getRole())) {
            throw new CustomException(ErrorCode.VALIDATION_FAILED); // 승인 대기 상태가 아님
        }

        user.setRole("ROLE_SELLER");

        // [NFR-002] 역할 승인은 감사 로그 대상 - 승인자(adminEmail)와 대상(email)을 함께 남긴다.
        systemLogService.log("MEMBER", "SUCCESS", "판매자 권한 승인: " + email + " (승인자: " + adminEmail + ")");
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

    /** 관리자용 블록체인 비동기 작업 상태 및 실패 이력 조회. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getBlockchainJobs() {
        return productRepository.findAll().stream()
                .sorted(java.util.Comparator.comparing(
                        Product::getBlockchainUpdatedAt,
                        java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                .map(product -> {
                    Map<String, Object> job = new HashMap<>();
                    job.put("productId", product.getProductId());
                    job.put("title", product.getTitle());
                    job.put("status", product.getBlockchainStatus());
                    job.put("txHash", product.getTxHash());
                    job.put("retryCount", product.getBlockchainRetryCount());
                    job.put("lastError", product.getBlockchainLastError());
                    job.put("updatedAt", product.getBlockchainUpdatedAt());
                    return job;
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

            if (!readResult.success() && !"NOT_FOUND".equals(readResult.code())) {
                BlockchainJobStatus failureStatus = "UNAVAILABLE".equals(readResult.code())
                        ? BlockchainJobStatus.FAILED_RETRYABLE
                        : BlockchainJobStatus.FAILED_FINAL;
                markLegacyBlockchainState(product, failureStatus, readResult.message());
                failedCount++;
                items.add(migrationItem(product.getProductId(), "RPC_ERROR",
                        product.getTxHash(), readResult.message()));
                processedCount++;
                notifyMigrationProgress(progressListener, totalCount, processedCount,
                        confirmedCount, failedCount, alreadySyncedCount,
                        remediatedCount, mismatchCount, product.getProductId());
                continue;
            }

            String hashOnChain = readResult.success() ? readResult.hash() : null;

            if (hashOnChain != null && !hashOnChain.isEmpty()) {
                if (productHashService.matches(expectedHash, hashOnChain)) {
                    markLegacyBlockchainState(product, BlockchainJobStatus.CONFIRMED, null);
                    alreadySyncedCount++;
                    items.add(migrationItem(product.getProductId(), "CONFIRMED",
                            product.getTxHash(), "온체인 해시와 일치하여 DB 상태를 보정했습니다."));
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
                    markLegacyBlockchainState(product, BlockchainJobStatus.FAILED_FINAL,
                            "기존 온체인 해시와 현재 DB 해시가 일치하지 않습니다.");
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

    private void markLegacyBlockchainState(
            Product product,
            BlockchainJobStatus status,
            String lastError) {
        product.setBlockchainStatus(status);
        if (product.getBlockchainRetryCount() == null) {
            product.setBlockchainRetryCount(0);
        }
        product.setBlockchainLastError(lastError);
        product.setBlockchainUpdatedAt(LocalDateTime.now());
        productRepository.save(product);
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
