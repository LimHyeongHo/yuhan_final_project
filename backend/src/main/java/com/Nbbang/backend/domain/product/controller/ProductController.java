package com.Nbbang.backend.domain.product.controller; // 🚨 본인 경로에 맞게 수정

import com.Nbbang.backend.domain.product.entity.Product;
import com.Nbbang.backend.domain.product.entity.Participation;
import com.Nbbang.backend.domain.product.entity.Scrap;
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
        String userId = (session != null) ? (String) session.getAttribute("userId") : null;
        if (userId == null) {
            throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
        }
        // [SEC-RQ-004] @ModelAttribute는 폼 데이터를 통째로 바인딩하므로, 서버가 관리해야 할 필드는
        // 클라이언트가 뭘 보냈든 무시하고 서버 쪽에서 강제로 안전한 초기값으로 되돌린다.
        // (안 그러면 상품 생성 요청에 status=CLOSED_SUCCESS, currentCount=999, txHash=아무값,
        //  sellerId=타인ID 등을 끼워 넣어도 그대로 저장됨)
        product.setSellerEmail(userId);
        product.setSellerId(null); // ProductService가 null이면 기본값으로 채움
        product.setCurrentCount(0);
        product.setStatus("OPEN");
        product.setTxHash(null);
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

    // [SEC-RQ-004] 본인 상품만 수정 가능 - sellerEmail이 세션 사용자와 일치해야 함
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(
            @PathVariable Long id,
            @ModelAttribute Product product,
            @RequestParam(value = "image", required = false) MultipartFile image,
            HttpSession session) {
        requireOwnership(id, session);
        Product updatedProduct = productService.updateProduct(id, product, image);
        return ResponseEntity.ok(updatedProduct);
    }

    // [SEC-RQ-004] 본인 상품만 삭제 가능 - sellerEmail이 세션 사용자와 일치해야 함
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id, HttpSession session) {
        requireOwnership(id, session);
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    // 세션 로그인 여부 + 상품 소유자(sellerEmail) 일치 여부를 확인. 익명은 401, 타인 상품이면 403.
    private void requireOwnership(Long productId, HttpSession session) {
        String userId = (session != null) ? (String) session.getAttribute("userId") : null;
        if (userId == null) {
            throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
        }
        Product product = productService.getProductById(productId);
        if (!userId.equals(product.getSellerEmail())) {
            throw new CustomException(ErrorCode.AUTH_ACCESS_DENIED);
        }
    }

    /** [신규] DB 해킹 시뮬레이션 (시연용) */
    @PostMapping("/{id}/simulate-hack")
    public ResponseEntity<Void> simulateHack(@PathVariable Long id) {
        productService.simulateDatabaseHack(id);
        return ResponseEntity.ok().build();
    }

    /** [신규] 구매자 마이페이지용 참여 내역 조회 */
    @GetMapping("/participations/me")
    public ResponseEntity<java.util.List<java.util.Map<String, Object>>> getMyBuyerParticipations(HttpSession session) {
        String email = (String) session.getAttribute("userId");
        if (email == null) {
            throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
        }
        java.util.List<java.util.Map<String, Object>> participations = productService.getMyParticipations(email);
        return ResponseEntity.ok(participations);
    }

    /** [신규] 스크랩 토글 (추가/취소) */
    @PostMapping("/{id}/scrap")
    public ResponseEntity<Boolean> toggleScrap(@PathVariable Long id, HttpSession session) {
        String email = getEmail(session);
        boolean isScrapped = productService.toggleScrap(id, email);
        return ResponseEntity.ok(isScrapped);
    }

    /** [신규] 특정 상품 스크랩 여부 조회 */
    @GetMapping("/{id}/scrap/status")
    public ResponseEntity<Boolean> checkScrapStatus(@PathVariable Long id, HttpSession session) {
        String email = getEmail(session);
        boolean isScrapped = productService.checkScrapStatus(id, email);
        return ResponseEntity.ok(isScrapped);
    }

    /** [신규] 마이페이지 전체 스크랩 내역 조회 */
    @GetMapping("/scraps/me")
    public ResponseEntity<java.util.List<java.util.Map<String, Object>>> getMyScraps(HttpSession session) {
        String email = getEmail(session);
        java.util.List<java.util.Map<String, Object>> scraps = productService.getMyScraps(email);
        return ResponseEntity.ok(scraps);
    }
    /** [신규] 판매자 주문 관리(명단 확인) 용 내역 조회 */
    @GetMapping("/seller/orders")
    public ResponseEntity<java.util.List<java.util.Map<String, Object>>> getSellerOrders(HttpSession session) {
        String email = getEmail(session);
        java.util.List<java.util.Map<String, Object>> orders = productService.getSellerOrders(email);
        return ResponseEntity.ok(orders);
    }
}