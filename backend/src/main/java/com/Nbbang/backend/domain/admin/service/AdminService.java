package com.Nbbang.backend.domain.admin.service;

import com.Nbbang.backend.domain.auth.entity.UserAccount;
import com.Nbbang.backend.domain.auth.repository.UserAccountRepository;
import com.Nbbang.backend.domain.notification.entity.Notification;
import com.Nbbang.backend.domain.notification.repository.NotificationRepository;
import com.Nbbang.backend.domain.product.entity.Product;
import com.Nbbang.backend.domain.product.repository.ProductRepository;
import com.Nbbang.backend.global.exception.CustomException;
import com.Nbbang.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserAccountRepository userAccountRepository;
    private final ProductRepository productRepository;
    private final NotificationRepository notificationRepository;

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
                map.put("status", product.getStatus());
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
                map.put("status", product.getStatus());
                map.put("seller", product.getSellerEmail() != null ? product.getSellerEmail() : "알 수 없음");
                map.put("date", product.getCreatedAt().toLocalDate().toString());
                
                int ratio = 0;
                if (product.getTargetCount() != null && product.getTargetCount() > 0) {
                    ratio = (int) (((double) product.getCurrentCount() / product.getTargetCount()) * 100);
                }
                map.put("ratio", ratio);
                
                // 프론트의 suspicious(이상 거래 의심) 모의 로직
                boolean isSuspicious = product.getPrice() != null && product.getPrice().intValue() > 500000;
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
        // 블록체인에 등록된(txHash가 있는) 상품 중 랜덤으로 하나 선택
        java.util.List<Product> validProducts = productRepository.findAll().stream()
                .filter(p -> p.getTxHash() != null && !p.getTxHash().isEmpty())
                .collect(Collectors.toList());

        if (validProducts.isEmpty()) {
            throw new CustomException(ErrorCode.VALIDATION_FAILED);
        }

        Product product = validProducts.get(new java.util.Random().nextInt(validProducts.size()));

        java.math.BigDecimal currentPrice = product.getPrice() != null ? product.getPrice() : java.math.BigDecimal.ZERO;
        String isbn = product.getIsbn() != null ? product.getIsbn() : "";
        
        // 원본 해시 계산
        String originalData = product.getProductId() + "-" + isbn + "-" + currentPrice.toString();
        String originalHash = calculateHash(originalData);

        // 고의로 가격을 10,000원 증가시켜 DB에 저장 (블록체인 우회)
        java.math.BigDecimal newPrice = currentPrice.add(new java.math.BigDecimal("10000"));
        product.setPrice(newPrice);
        productRepository.save(product);

        // 변조된 해시 계산
        String newData = product.getProductId() + "-" + isbn + "-" + newPrice.toString();
        String newHash = calculateHash(newData);

        java.util.Map<String, Object> result = new HashMap<>();
        result.put("productId", product.getProductId());
        result.put("originalPrice", currentPrice);
        result.put("newPrice", newPrice);
        result.put("originalHash", originalHash);
        result.put("newHash", newHash);

        return result;
    }

    private String calculateHash(String input) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return "ERROR";
        }
    }

    // [신규] 보안 검증 시뮬레이터: 정상 복구
    @Transactional
    public void restoreHack(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.VALIDATION_FAILED));
        
        // 조작된 가격 10,000원 원상복구
        java.math.BigDecimal currentPrice = product.getPrice() != null ? product.getPrice() : java.math.BigDecimal.ZERO;
        product.setPrice(currentPrice.subtract(new java.math.BigDecimal("10000")));
        productRepository.save(product);
    }
}
