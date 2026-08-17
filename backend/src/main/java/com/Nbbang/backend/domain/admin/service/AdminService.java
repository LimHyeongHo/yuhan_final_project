package com.Nbbang.backend.domain.admin.service;

import com.Nbbang.backend.domain.auth.entity.UserAccount;
import com.Nbbang.backend.domain.auth.repository.UserAccountRepository;
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
}
