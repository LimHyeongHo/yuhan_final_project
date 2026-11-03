package com.Nbbang.backend.domain.report.service;

import com.Nbbang.backend.domain.auth.entity.UserAccount;
import com.Nbbang.backend.domain.auth.repository.UserAccountRepository;
import com.Nbbang.backend.domain.product.entity.Product;
import com.Nbbang.backend.domain.product.repository.ProductRepository;
import com.Nbbang.backend.domain.notification.entity.Notification;
import com.Nbbang.backend.domain.notification.repository.NotificationRepository;
import com.Nbbang.backend.domain.report.dto.ProductReportCreateRequest;
import com.Nbbang.backend.domain.report.entity.ProductReport;
import com.Nbbang.backend.domain.report.entity.UserReportCount;
import com.Nbbang.backend.domain.report.repository.ProductReportRepository;
import com.Nbbang.backend.domain.report.repository.UserReportCountRepository;
import com.Nbbang.backend.global.exception.CustomException;
import com.Nbbang.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

@Service
@RequiredArgsConstructor
public class ProductReportService {

    private static final Set<String> ALLOWED_REASONS = Set.of(
            "상품 정보와 다름",
            "부적절한 상품 설명",
            "거래 조건 미고지",
            "기타");

    private static final Set<String> MANAGEABLE_STATUSES = Set.of("RECEIVED", "REJECTED");

    private final ProductRepository productRepository;
    private final UserAccountRepository userAccountRepository;
    private final ProductReportRepository productReportRepository;
    private final UserReportCountRepository userReportCountRepository;
    private final NotificationRepository notificationRepository;

    @Transactional
    public void createReport(Long productId, String reporterEmail, ProductReportCreateRequest request) {
        if (request == null) {
            throw new CustomException(ErrorCode.VALIDATION_FAILED);
        }
        String reason = request.reason() == null ? "" : request.reason().trim();
        String detail = request.detail() == null ? null : request.detail().trim();
        if (!ALLOWED_REASONS.contains(reason) || (detail != null && detail.length() > 1000)) {
            throw new CustomException(ErrorCode.VALIDATION_FAILED);
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
        if (product.getSellerEmail() == null) {
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        }
        if (reporterEmail.equals(product.getSellerEmail())) {
            throw new CustomException(ErrorCode.AUTH_ACCESS_DENIED);
        }
        if (productReportRepository.existsByReporter_EmailAndProduct_ProductId(reporterEmail, productId)) {
            throw new CustomException(ErrorCode.PRODUCT_DUPLICATE);
        }

        UserAccount reporter = userAccountRepository.findById(reporterEmail)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        UserAccount seller = userAccountRepository.findById(product.getSellerEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        ProductReport report = new ProductReport();
        report.setReporter(reporter);
        report.setProduct(product);
        report.setSeller(seller);
        report.setReason(reason);
        report.setDetail(detail == null || detail.isBlank() ? null : detail);
        productReportRepository.save(report);

        UserReportCount reportCount = userReportCountRepository.findById(seller.getEmail())
                .orElseGet(() -> {
                    UserReportCount newReportCount = new UserReportCount();
                    newReportCount.setMemberEmail(seller.getEmail());
                    return newReportCount;
                });
        reportCount.setPostReportCount(reportCount.getPostReportCount() + 1);
        userReportCountRepository.save(reportCount);
    }

    @Transactional
    public void updateReportStatus(Long reportId, String status) {
        if (status == null || !MANAGEABLE_STATUSES.contains(status)) {
            throw new CustomException(ErrorCode.VALIDATION_FAILED);
        }

        ProductReport report = productReportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
        if (!"PENDING".equals(report.getStatus())) {
            throw new CustomException(ErrorCode.ADMIN_APPROVAL_ALREADY_PROCESSED);
        }
        report.setStatus(status);

        if ("RECEIVED".equals(status)) {
            String productName = report.getProduct().getTitle();
            String sellerMessage = "등록하신 상품 '" + productName + "'에 대한 신고가 관리자에 의해 접수되었습니다.";
            notificationRepository.save(new Notification(report.getSeller().getEmail(), sellerMessage));

            userAccountRepository.findByRoleOrderByCreatedAtDesc("ROLE_ADMIN")
                    .forEach(admin -> notificationRepository.save(new Notification(
                            admin.getEmail(),
                            "상품 신고가 접수 처리되었습니다. 판매자: " + report.getSeller().getNickname() + ", 상품: " + productName)));
        }
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllReports() {
        return productReportRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(report -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("id", report.getProductReportId());
                    result.put("sellerEmail", report.getSeller().getEmail());
                    result.put("sellerName", report.getSeller().getNickname());
                    result.put("productName", report.getProduct().getTitle());
                    result.put("reason", report.getReason());
                    result.put("detail", report.getDetail());
                    result.put("statusCode", report.getStatus());
                    result.put("reportedAt", report.getCreatedAt().toLocalDate().toString());
                    result.put("status", "PENDING".equals(report.getStatus()) ? "검토 대기" : report.getStatus());
                    return result;
                })
                .toList();
    }
}
