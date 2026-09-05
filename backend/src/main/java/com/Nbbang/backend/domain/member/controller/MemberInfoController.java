package com.Nbbang.backend.domain.member.controller;

import com.Nbbang.backend.domain.auth.entity.DeviceCert;
import com.Nbbang.backend.domain.auth.entity.UserAccount;
import com.Nbbang.backend.domain.auth.repository.DeviceCertRepository;
import com.Nbbang.backend.domain.auth.repository.UserAccountRepository;
import com.Nbbang.backend.domain.auth.service.CAService;
import com.Nbbang.backend.domain.log.service.SystemLogService;
import com.Nbbang.backend.domain.member.service.CertificateSessionService;
import com.Nbbang.backend.domain.payment.entity.Payment;
import com.Nbbang.backend.domain.payment.repository.PaymentRepository;
import com.Nbbang.backend.domain.product.entity.Participation;
import com.Nbbang.backend.domain.product.entity.Product;
import com.Nbbang.backend.domain.product.repository.ParticipationRepository;
import com.Nbbang.backend.domain.product.repository.ProductRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/member")
// CORS는 SecurityConfig의 CorsConfigurationSource가 자격증명 포함으로 처리 (세션 쿠키 필요)
public class MemberInfoController {

    private static final DateTimeFormatter CREATED_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH:mm");
    // 프론트엔드 회원가입/마이페이지 비밀번호 검증 규칙과 동일 (영문+숫자+특수문자 조합, 8자 이상)
    private static final Pattern PASSWORD_COMPOSITION_REGEX =
            Pattern.compile("^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).+$");

    private final UserAccountRepository userAccountRepository;
    private final DeviceCertRepository deviceCertRepository;
    private final CertificateSessionService certificateSessionService;
    private final CAService caService;
    private final PasswordEncoder passwordEncoder;
    private final ProductRepository productRepository;
    private final SystemLogService systemLogService;
    private final ParticipationRepository participationRepository;
    private final PaymentRepository paymentRepository;

    // [MEM-RQ-001] 탈퇴 시 거래 이력은 보존하되 개인 표시 정보만 이 값으로 덮어써 익명화한다.
    private static final String ANONYMIZED_DISPLAY_NAME = "탈퇴한 사용자";

    public MemberInfoController(UserAccountRepository userAccountRepository,
                                 DeviceCertRepository deviceCertRepository,
                                 CertificateSessionService certificateSessionService,
                                 CAService caService,
                                 PasswordEncoder passwordEncoder,
                                 ProductRepository productRepository,
                                 SystemLogService systemLogService,
                                 ParticipationRepository participationRepository,
                                 PaymentRepository paymentRepository) {
        this.userAccountRepository = userAccountRepository;
        this.deviceCertRepository = deviceCertRepository;
        this.certificateSessionService = certificateSessionService;
        this.caService = caService;
        this.passwordEncoder = passwordEncoder;
        this.productRepository = productRepository;
        this.systemLogService = systemLogService;
        this.participationRepository = participationRepository;
        this.paymentRepository = paymentRepository;
    }

    // 마이페이지 "회원 정보 개요"에 표시할 실제 계정 정보 + CA 인증서 시리얼 번호 조회
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info(HttpServletRequest request) {
        try {
            String userId = requireUserId(request);
            UserAccount user = userAccountRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("존재하지 않는 계정입니다."));

            Map<String, Object> body = new HashMap<>();
            body.put("email", user.getEmail());
            body.put("nickname", user.getNickname());
            body.put("role", user.getRole());
            body.put("createdAt", user.getCreatedAt().format(CREATED_AT_FORMAT));
            deviceCertRepository.findByUserId(userId).ifPresent(cert -> {
                body.put("certificateSerialNumber", cert.getCertificateSerialNumber());
                if (cert.getCertificateIssuedAt() != null) {
                    body.put("certificateIssuedAt", cert.getCertificateIssuedAt().toString());
                }
                if (cert.getCertificateExpiresAt() != null) {
                    body.put("certificateExpiresAt", cert.getCertificateExpiresAt().toString());
                }
            });

            return ResponseEntity.ok(body);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    // 마이페이지 "프로필/비밀번호 수정" 저장: 닉네임/비밀번호 부분 수정 (빈 값이면 그대로 유지)
    @PatchMapping("/profile")
    public ResponseEntity<Map<String, Object>> updateProfile(@RequestBody Map<String, String> request,
                                                               HttpServletRequest httpRequest) {
        try {
            String userId = requireUserId(httpRequest);
            UserAccount user = userAccountRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("존재하지 않는 계정입니다."));

            String nickname = request.get("nickname");
            if (nickname != null && !nickname.trim().isEmpty()) {
                user.setNickname(nickname.trim());
            }

            String newPassword = request.get("newPassword");
            if (newPassword != null && !newPassword.isEmpty()) {
                if (newPassword.length() < 8 || !PASSWORD_COMPOSITION_REGEX.matcher(newPassword).matches()) {
                    throw new RuntimeException("비밀번호는 8자 이상, 영문/숫자/특수문자를 모두 포함해야 합니다.");
                }
                user.setPassword(passwordEncoder.encode(newPassword));
            }

            userAccountRepository.save(user);
            httpRequest.getSession().setAttribute("nickname", user.getNickname());

            Map<String, Object> body = new HashMap<>();
            body.put("nickname", user.getNickname());
            body.put("message", "회원 정보가 수정되었습니다.");
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    // 마이페이지에서 로그인된 상태로 바로 인증서 재발급 (본인인증 재실행 없이, 이미 저장된 ci_hash를 그대로 유지)
    @PostMapping("/certificate/reissue")
    @Transactional
    public ResponseEntity<Map<String, Object>> reissueCertificate(@RequestBody Map<String, String> request,
                                                                    HttpServletRequest httpRequest) {
        try {
            String userId = requireUserId(httpRequest);
            String publicKey = request.get("publicKey");
            String deviceId = request.get("deviceId");
            if (publicKey == null || publicKey.isEmpty()) throw new RuntimeException("공개키가 필요합니다.");
            if (deviceId == null || deviceId.isEmpty()) throw new RuntimeException("기기 ID가 필요합니다.");

            DeviceCert cert = deviceCertRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("발급된 인증서가 없습니다."));

            byte[] encodedPublicKey = Base64.getDecoder().decode(publicKey);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey devicePublicKey = keyFactory.generatePublic(new X509EncodedKeySpec(encodedPublicKey));
            X509Certificate certificate = caService.issueDeviceCertificate(devicePublicKey, deviceId);
            String serialNumber = certificate.getSerialNumber().toString();

            LocalDateTime certIssuedAt = toLocalDateTime(certificate.getNotBefore());
            LocalDateTime certExpiresAt = toLocalDateTime(certificate.getNotAfter());

            cert.setDeviceId(deviceId);
            cert.setPublicKey(publicKey);
            cert.setCertificateSerialNumber(serialNumber);
            cert.setRevoked(false);
            cert.setCertificateIssuedAt(certIssuedAt);
            cert.setCertificateExpiresAt(certExpiresAt);
            deviceCertRepository.save(cert);

            certificateSessionService.startSession(userId); // 10분 유효 타이머 재시작

            Map<String, Object> body = new HashMap<>();
            body.put("certificateSerialNumber", serialNumber);
            body.put("certificateIssuedAt", certIssuedAt.toString());
            body.put("certificateExpiresAt", certExpiresAt.toString());
            body.put("message", "인증서가 재발급되었습니다.");
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    // 회원 탈퇴 (MEM-RQ-001, MEM-RQ-002)
    // - CA 인증서 폐기 (기기 인증서는 삭제하지 않고 revoked=true로만 표시해 감사 이력 보존)
    // - 계정은 물리 삭제 대신 status=WITHDRAWN으로 비활성화 (거래/결제/후기 감사 이력 보존)
    // - 판매자였다면 모집 중이던 상품을 즉시 거래 불가 상태(SELLER_WITHDRAWN)로 전환
    // - 세션 종료
    // 전부 하나의 트랜잭션 안에서 처리되어, 중간에 실패하면 전체가 롤백된다.
    @DeleteMapping("/withdraw")
    @Transactional
    public ResponseEntity<Map<String, String>> withdraw(HttpServletRequest request) {
        try {
            String userId = requireUserId(request);
            UserAccount user = userAccountRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("존재하지 않는 계정입니다."));

            certificateSessionService.revoke(userId);

            for (Product product : productRepository.findBySellerEmailOrderByCreatedAtDesc(userId)) {
                if ("OPEN".equals(product.getStatus()) || "FULL".equals(product.getStatus())) {
                    product.setStatus("SELLER_WITHDRAWN");
                }
            }

            // [MEM-RQ-001] 참여/결제 기록(감사 데이터)은 그대로 두되, 화면에 노출되는 닉네임 스냅샷만 익명화
            for (Participation participation : participationRepository.findByMember_EmailOrderByJoinDateDesc(userId)) {
                participation.setBuyerName(ANONYMIZED_DISPLAY_NAME);
            }
            for (Payment payment : paymentRepository.findByMember_Email(userId)) {
                payment.setBuyerName(ANONYMIZED_DISPLAY_NAME);
            }

            user.setStatus("WITHDRAWN");
            userAccountRepository.save(user);

            // [NFR-002] 회원 탈퇴는 감사 로그 대상
            systemLogService.log("MEMBER", "SUCCESS", "회원 탈퇴: " + userId);

            request.getSession().invalidate();

            Map<String, String> response = new HashMap<>();
            response.put("message", "회원 탈퇴가 완료되었습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    private static LocalDateTime toLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private String requireUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            throw new RuntimeException("로그인이 필요합니다.");
        }
        return (String) session.getAttribute("userId");
    }
}
