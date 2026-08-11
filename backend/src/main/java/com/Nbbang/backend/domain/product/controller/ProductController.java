package com.Nbbang.backend.domain.product.controller; // 🚨 본인 경로에 맞게 수정

import com.Nbbang.backend.domain.product.entity.Product;
import com.Nbbang.backend.domain.product.entity.Participation;
import com.Nbbang.backend.domain.product.service.ProductService;
import com.Nbbang.backend.global.exception.CustomException;
import com.Nbbang.backend.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class ProductController {

    private final ProductService productService; // 💡 레포지토리 대신 서비스로 변경!

    @PostMapping
    public ResponseEntity<Product> createProduct(
            @ModelAttribute Product product,
            @RequestParam(value = "image", required = false) MultipartFile image,
            HttpSession session) {
        // 프론트엔드에서 FormData로 전송하므로 @ModelAttribute로 매핑하고 이미지는 별도로 받습니다.
        // [신규] POST /api/chat/rooms 호출용 판매자 이메일 세팅
        product.setSellerEmail((String) session.getAttribute("userId"));
        Product savedProduct = productService.createProduct(product, image);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedProduct);
    }

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        List<Product> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        Product product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<Product> joinProduct(@PathVariable Long id, HttpServletRequest request) {
        String userId = requireUserId(request);
        Product product = productService.joinProduct(id, userId);
        return ResponseEntity.ok(product);
    }

    // 로그인 여부 확인: 세션에 userId가 없으면 참여 인원 조작(비로그인 상태로 join 호출)을 막기 위해 거부
    private String requireUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
        }
        return (String) session.getAttribute("userId");
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Product> cancelJoinProduct(@PathVariable Long id) {
        Product product = productService.cancelJoinProduct(id);
        return ResponseEntity.ok(product);
    }

    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<List<Product>> getProductsBySellerId(@PathVariable Long sellerId) {
        List<Product> products = productService.getProductsBySellerId(sellerId);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/seller/{sellerId}/participations")
    public ResponseEntity<List<Map<String, Object>>> getParticipationsBySellerId(@PathVariable Long sellerId) {
        return ResponseEntity.ok(productService.getParticipationsBySellerId(sellerId));
    }

    /** [신규] 로그인한 판매자 본인의 상품 목록 (sellerId 대신 sellerEmail 기준) */
    @GetMapping("/seller/me")
    public ResponseEntity<List<Product>> getMyProducts(HttpSession session) {
        List<Product> products = productService.getProductsBySellerEmail(getEmail(session));
        return ResponseEntity.ok(products);
    }

    /** [신규] 로그인한 판매자 본인 상품에 대한 최근 참여자 내역 */
    @GetMapping("/seller/me/participations")
    public ResponseEntity<List<Map<String, Object>>> getMyParticipations(HttpSession session) {
        return ResponseEntity.ok(productService.getParticipationsBySellerEmail(getEmail(session)));
    }

    /** 세션에서 userId(email) 추출 — 로그인 안 된 경우 401 */
    private String getEmail(HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
        return userId;
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(
            @PathVariable Long id,
            @ModelAttribute Product product,
            @RequestParam(value = "image", required = false) MultipartFile image) {
        Product updatedProduct = productService.updateProduct(id, product, image);
        return ResponseEntity.ok(updatedProduct);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
