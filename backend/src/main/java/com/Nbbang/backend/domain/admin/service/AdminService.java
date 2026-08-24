package com.Nbbang.backend.domain.admin.service;

import com.Nbbang.backend.domain.auth.entity.UserAccount;
import com.Nbbang.backend.domain.auth.repository.UserAccountRepository;
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

    @Transactional
    public void makeAdminTemp(String email) {
        userAccountRepository.findById(email).ifPresent(user -> {
            user.setRole("ROLE_ADMIN");
        });
    }
}
