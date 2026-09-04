package com.Nbbang.backend.domain.product.service;

import com.Nbbang.backend.domain.product.entity.Product;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class ProductHashService {

    public String buildDataString(Product product) {
        return buildDataString(product.getProductId(), product.getIsbn(), product.getPrice());
    }

    public String buildDataString(Long productId, String isbn, BigDecimal price) {
        String normalizedIsbn = isbn != null ? isbn : "";
        String normalizedPrice = price != null ? price.stripTrailingZeros().toPlainString() : "";
        return productId + "_" + normalizedIsbn + "_" + normalizedPrice;
    }

    public String calculateHash(Product product) {
        return calculateHash(product.getProductId(), product.getIsbn(), product.getPrice());
    }

    public String calculateHash(Long productId, String isbn, BigDecimal price) {
        return hashString(buildDataString(productId, isbn, price));
    }

    /**
     * 기존 관리자 마이그레이션이 사용하던 형식입니다.
     * 이미 잘못 기록된 데이터를 식별하여 정규 규격으로 재기록할 때만 사용합니다.
     */
    public String calculateLegacyMigrationHash(Product product) {
        String isbn = product.getIsbn() != null ? product.getIsbn() : "";
        BigDecimal price = product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;
        return hashString(product.getProductId() + "-" + isbn + "-" + price.toString());
    }

    public boolean matches(String expectedHash, String actualHash) {
        if (expectedHash == null || actualHash == null) {
            return false;
        }
        return normalizeHash(expectedHash).equals(normalizeHash(actualHash));
    }

    private String hashString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();

            for (byte hashByte : hashBytes) {
                String hex = Integer.toHexString(0xff & hashByte);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("상품 해시 생성 중 오류가 발생했습니다.", ex);
        }
    }

    private String normalizeHash(String hash) {
        return hash.replace("0x", "")
                .trim()
                .toLowerCase()
                .replaceAll("[^a-f0-9]", "");
    }
}
