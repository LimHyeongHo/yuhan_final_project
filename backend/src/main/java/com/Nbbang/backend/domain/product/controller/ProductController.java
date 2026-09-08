package com.Nbbang.backend.domain.product.controller; // 🚨 본인 경로에 맞게 수정

import com.Nbbang.backend.domain.payment.service.PaymentService;
import com.Nbbang.backend.domain.product.entity.Product;
import com.Nbbang.backend.domain.product.entity.Participation;
import com.Nbbang.backend.domain.product.entity.Scrap;
import com.Nbbang.backend.domain.product.repository.ParticipationRepository;
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
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class ProductController {

    private static final long REGISTRATION_KEY_TTL_MILLIS = 5 * 60 * 1000L;
    private final Map<String, Long> recentRegistrationKeys = new ConcurrentHashMap<>();

    private final ProductService productService; // 💡 레포지토리 대신 서비스로 변경!
    private final PaymentService paymentService; // [신규] 참여 취소(환불 포함) 오케스트레이션용
    private final ParticipationRepository participationRepository; // [신규] 참여 여부 조회용

    @PostMapping
    public ResponseEntity<Product> createProduct(
            @ModelAttribute Product product,
            @RequestParam(value = "image", required = false) MultipartFile image,
            HttpSession session,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            String userId = String.valueOf(session.getAttribute("userId"));
            String requestKey = userId + ":" + idempotencyKey.trim();
            long now = System.currentTimeMillis();
            recentRegistrationKeys.entrySet().removeIf(entry -> entry.getValue() <= now);
            if (recentRegistrationKeys.putIfAbsent(
                    requestKey, now + REGISTRATION_KEY_TTL_MILLIS) != null) {
                throw new CustomException(ErrorCode.PRODUCT_DUPLICATE);
            }
        }
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

    // [수정] PRD-RQ-001/PAY-RQ-001: 인증 필수 + 본인 Participation/Payment를 함께 처리하는
    // paymentService.cancelParticipation으로 위임 (예전엔 인증 없이 productId만으로 인원수를 깎았음)
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Product> cancelJoinProduct(@PathVariable Long id, HttpServletRequest request) {
        String userId = requireUserId(request);
        Product product = paymentService.cancelParticipation(id, userId, "구매자 요청");
        return ResponseEntity.ok(product);
    }

    // [신규] 로그인한 사용자가 이 상품에 실제로 참여 중인지 여부 (scrap/status와 동일 패턴)
    @GetMapping("/{id}/participation/status")
    public ResponseEntity<Boolean> checkParticipationStatus(@PathVariable Long id, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            return ResponseEntity.ok(false);
        }
        String email = (String) session.getAttribute("userId");
        boolean joined = participationRepository.existsByProduct_ProductIdAndMember_Email(id, email);
        return ResponseEntity.ok(joined);
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
            @RequestParam(value = "image", required = false) MultipartFile image,
            HttpSession session) {
        Product updatedProduct = productService.updateProduct(id, product, image, getEmail(session));
        return ResponseEntity.ok(updatedProduct);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
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
