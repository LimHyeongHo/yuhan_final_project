package com.Nbbang.backend.domain.auth.controller;

import com.Nbbang.backend.domain.auth.entity.DeviceCert;
import com.Nbbang.backend.domain.auth.entity.UserAccount;
import com.Nbbang.backend.domain.auth.repository.DeviceCertRepository;
import com.Nbbang.backend.domain.auth.repository.UserAccountRepository;
import com.Nbbang.backend.domain.auth.service.CAService;
import com.Nbbang.backend.domain.auth.service.PkiService;
import com.Nbbang.backend.domain.member.service.CertificateSessionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/pki")
// [수정] 세션 쿠키(JSESSIONID)를 자격증명 포함 CORS로 처리하기 위해 SecurityConfig의 CorsConfigurationSource로 일원화 (와일드카드 CrossOrigin 제거)
public class PkiController {

    private final PkiService pkiService;
    private final UserAccountRepository userAccountRepository;
    private final DeviceCertRepository deviceCertRepository;
    private final CAService caService;
    private final CertificateSessionService certificateSessionService;

    @Value("${portone.api-secret}")
    private String portoneApiSecret;

    @Value("${portone.api.base-url}")
    private String portoneBaseUrl;

    // 생성자 주입
    public PkiController(PkiService pkiService,
                         UserAccountRepository userAccountRepository,
                         DeviceCertRepository deviceCertRepository,
                         CAService caService,
                         CertificateSessionService certificateSessionService) {
        this.pkiService = pkiService;
        this.userAccountRepository = userAccountRepository;
        this.deviceCertRepository = deviceCertRepository;
        this.caService = caService;
        this.certificateSessionService = certificateSessionService;
    }

    /**
     * 포트원 본인인증 결과 검증
     */
    @PostMapping("/verify-portone")
    public ResponseEntity<?> verifyPortone(@RequestBody Map<String, String> request) {
        String identityVerificationId = request.get("identityVerificationId");
        
        try {
            org.springframework.web.reactive.function.client.WebClient webClient = 
                org.springframework.web.reactive.function.client.WebClient.builder()
                    .baseUrl(portoneBaseUrl)
                    .defaultHeader("Authorization", "PortOne " + portoneApiSecret)
                    .build();

            Map<String, Object> response = webClient.get()
                    .uri("/identity-verifications/" + identityVerificationId)
                    .retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            System.out.println("PortOne API Response for " + identityVerificationId + ": " + response);

            if (response != null && "VERIFIED".equals(response.get("status"))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> verifiedCustomer = (Map<String, Object>) response.get("verifiedCustomer");
                
                if (verifiedCustomer == null) {
                    throw new RuntimeException("인증 데이터(verifiedCustomer)가 응답에 포함되어 있지 않습니다.");
                }

                String name = (String) verifiedCustomer.get("name");
                String ci = (String) verifiedCustomer.get("ci");

                // KG이니시스 테스트 모드 등에서 CI가 안 들어올 경우 처리
                if (ci == null || ci.trim().isEmpty()) {
                    System.out.println("⚠️ [테스트 모드 감지] CI가 누락되어 고정된 임시 CI를 생성합니다.");
                    
                    String birthday = (String) verifiedCustomer.get("birthDate");
                    String phone = (String) verifiedCustomer.get("phoneNumber");
                    
                    String seed = (name != null ? name : "") + 
                                  (birthday != null ? birthday : "1990-01-01") + 
                                  (phone != null ? phone : "01000000000");
                    
                    ci = "STABLE_TEST_CI_" + java.util.Base64.getEncoder().encodeToString(seed.getBytes());
                }

                Map<String, String> result = new HashMap<>();
                result.put("name", name != null ? name : "테스트 사용자");
                result.put("ci", ci);
                return ResponseEntity.ok(result);
            } else {
                String reason = response != null ? String.valueOf(response.get("cancellationReason")) : "알 수 없음";
                throw new RuntimeException("본인인증 미완료 상태입니다. (사유: " + reason + ")");
            }
        } catch (Exception e) {
            System.err.println("PortOne verification failed: " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "포트원 검증 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 이메일(아이디) 중복 여부 확인 - 회원가입 폼 실시간 검증용
     */
    @GetMapping("/check-email")
    public ResponseEntity<Map<String, Boolean>> checkEmail(@RequestParam String email) {
        String normalizedEmail = email != null ? email.replaceAll("\\s", "") : "";
        boolean available = normalizedEmail.isEmpty() || !userAccountRepository.existsById(normalizedEmail);

        Map<String, Boolean> response = new HashMap<>();
        response.put("available", available);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email") != null ? request.get("email").replaceAll("\\s", "") : null;
            String password = request.get("password");
            String nickname = request.get("nickname");
            String role = request.get("role") != null ? request.get("role").trim() : "ROLE_USER";
            String ci = request.get("ci");
            String publicKey = request.get("publicKey");
            String deviceId = request.get("deviceId") != null ? request.get("deviceId").replaceAll("\\s", "") : email;

            if (email == null || email.isEmpty()) throw new RuntimeException("이메일을 입력해주세요.");

            System.out.println("Processing registration/update for: [" + email + "]");

            // --- 중복 가입 방지 로직 (1인 1계정 정책) ---
            String ciHash = pkiService.generateCiHash(ci);
            deviceCertRepository.findByCiHash(ciHash).ifPresent(existingCert -> {
                if (!existingCert.getUserId().equals(email)) {
                    throw new RuntimeException("이미 이 본인인증 정보로 가입된 다른 계정(" + existingCert.getUserId() + ")이 존재합니다.");
                }
            });
            // ------------------------------------------

            // 1. UserAccount 처리
            UserAccount userAccount = userAccountRepository.findById(email).orElse(null);
            boolean isNewUser = (userAccount == null);

            if (isNewUser) {
                // 신규 가입은 닉네임이 반드시 필요함. 재발급 요청(닉네임 빈 값)이
                // 미가입 이메일로 들어오면 NOT NULL 위반 대신 명확한 에러로 안내.
                if (nickname == null || nickname.trim().isEmpty()) {
                    throw new RuntimeException("가입되지 않은 계정입니다. 회원가입을 먼저 진행해주세요.");
                }
                userAccount = new UserAccount();
                userAccount.setEmail(email);
                userAccount.setPassword(password);
                userAccount.setNickname(nickname);
                if (request.get("role") != null && !request.get("role").trim().isEmpty()) {
                    userAccount.setRole(role);
                }
            } else if (nickname != null && !nickname.trim().isEmpty()) {
                // 이미 가입된 이메일로 신규 회원가입(닉네임 포함) 요청이 들어온 경우.
                // 기존에는 여기서 그대로 통과시켜 기존 계정의 비밀번호를 덮어썼음(계정 탈취 가능) -> 거부로 변경.
                throw new RuntimeException("이미 사용 중인 이메일입니다.");
            } else {
                // 재발급/기기 재등록: 닉네임 없이 기존 계정에 대한 요청.
                // 기존 비밀번호와 일치하는지 반드시 확인해야 함 - 확인 없이 통과시키면
                // 이메일만 알아도 아무 비밀번호로 계정을 탈취할 수 있는 심각한 취약점이 됨.
                if (password == null || !userAccount.getPassword().equals(password)) {
                    throw new RuntimeException("비밀번호가 일치하지 않습니다.");
                }
                // 검증 용도로만 사용하고, 재발급 과정에서 비밀번호 자체는 변경하지 않음.
            }

            userAccountRepository.saveAndFlush(userAccount);

            // 2. 인증서 직접 발급 (HTTP 대신 caService를 로컬 호출)
            byte[] encodedPublicKey = java.util.Base64.getDecoder().decode(publicKey);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey devicePublicKey = keyFactory.generatePublic(new X509EncodedKeySpec(encodedPublicKey));
            java.security.cert.X509Certificate certificate = caService.issueDeviceCertificate(devicePublicKey, deviceId);

            Map<String, Object> caResponse = new HashMap<>();
            caResponse.put("certificate", java.util.Base64.getEncoder().encodeToString(certificate.getEncoded()));
            String serialNumber = certificate.getSerialNumber().toString();
            caResponse.put("serialNumber", serialNumber);

            // 3. DeviceCert 처리 - 1인 1기기 정책 적용 (덮어쓰기)
            DeviceCert cert = deviceCertRepository.findByUserId(email).orElse(new DeviceCert());
            
            System.out.println("Updating device cert for: " + email + " -> New Device: " + deviceId);
            
            cert.setUserId(email);
            cert.setDeviceId(deviceId);
            cert.setPublicKey(publicKey);
            cert.setCiHash(ciHash);
            cert.setCertificateSerialNumber(serialNumber);
            cert.setPassword(password);
            cert.setRevoked(false);
            deviceCertRepository.saveAndFlush(cert);

            System.out.println("Successfully updated policy in DB: " + email + " (Active Device: " + deviceId + ")");
            return ResponseEntity.ok(caResponse);
        } catch (Exception e) {
            System.err.println("Registration failed: " + e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    // PKI(개인키/서명) 없이 이메일+비밀번호만으로 로그인 가능한 테스트 전용 계정 목록.
    // 실제 회원가입 계정은 이 목록에 없으므로 반드시 PKI 로그인을 거쳐야 함 (보안 우회 방지).
    private static final java.util.Set<String> TEST_LOGIN_ACCOUNTS = java.util.Set.of(
            "admin@naver.com",
            "seller01@test.com", "seller02@test.com",
            "buyer01@test.com", "buyer02@test.com"
    );

    @PostMapping("/admin/login")
    public ResponseEntity<Map<String, Object>> adminLogin(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        String email = request.get("email");
        String password = request.get("password");

        try {
            UserAccount user = userAccountRepository.findById(email)
                    .orElseThrow(() -> new RuntimeException("존재하지 않는 계정입니다."));

            if (!TEST_LOGIN_ACCOUNTS.contains(email)) {
                throw new RuntimeException("테스트 전용 계정만 이 방식으로 로그인할 수 있습니다.");
            }

            if (!user.getPassword().equals(password)) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "비밀번호가 일치하지 않습니다.");
                return ResponseEntity.status(401).body(response);
            }

            HttpSession session = httpRequest.getSession(true);
            session.setAttribute("userId", user.getEmail());
            session.setAttribute("nickname", user.getNickname());
            session.setAttribute("role", user.getRole());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("nickname", user.getNickname());
            response.put("role", user.getRole());
            response.put("message", user.getNickname() + "님 테스트 로그인 성공!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(401).body(response);
        }
    }

    @GetMapping("/login/challenge")
    public ResponseEntity<Map<String, String>> getChallenge(@RequestParam String deviceId) {
        String challenge = pkiService.createChallenge(deviceId);
        Map<String, String> response = new HashMap<>();
        response.put("challenge", challenge);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login/verify")
    public ResponseEntity<Map<String, Object>> verify(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        String deviceId = request.get("deviceId") != null ? request.get("deviceId").replaceAll("\\s", "") : "";
        String password = request.get("password");
        String signature = request.get("signature");

        System.out.println("Login verification attempt for device: [" + deviceId + "]");

        try {
            // 1. PKI 서명 검증 및 기기 정보 조회
            DeviceCert cert = deviceCertRepository.findByDeviceId(deviceId)
                    .orElseThrow(() -> new RuntimeException("기기 인증 정보가 없습니다."));
            
            // 2. 해당 기기와 연결된 사용자 계정 및 비밀번호 확인
            UserAccount user = userAccountRepository.findById(cert.getUserId())
                    .orElseThrow(() -> new RuntimeException("등록되지 않은 사용자입니다."));

            if (!user.getPassword().equals(password)) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "비밀번호가 일치하지 않습니다.");
                return ResponseEntity.status(401).body(response);
            }

            // 3. PKI 서명 검증 (기기 인증)
            boolean isValid = pkiService.validateChallenge(deviceId, signature);

            Map<String, Object> response = new HashMap<>();
            if (isValid) {
                HttpSession session = httpRequest.getSession(true);
                session.setAttribute("userId", user.getEmail());
                session.setAttribute("nickname", user.getNickname());
                session.setAttribute("role", user.getRole());

                // 로그인 성공 시 인증서 유효 타이머(기본 10분) 시작
                certificateSessionService.startSession(user.getEmail());

                response.put("success", true);
                response.put("nickname", user.getNickname());
                response.put("role", user.getRole());
                response.put("message", "기기 인증 및 로그인 성공!");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "기기 인증 실패: 폐기된 인증서이거나 서명이 올바르지 않습니다.");
                return ResponseEntity.status(401).body(response);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "로그인 오류: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/revoke")
    public ResponseEntity<Map<String, String>> revoke(@RequestBody Map<String, String> request) {
        try {
            String deviceId = request.get("deviceId");
            DeviceCert cert = deviceCertRepository.findByDeviceId(deviceId)
                    .orElseThrow(() -> new RuntimeException("해당 기기의 인증서 정보를 찾을 수 없습니다."));

            // 로컬 caService 직접 호출하여 인증서 폐기
            caService.revokeCertificate(new BigInteger(cert.getCertificateSerialNumber()));

            cert.setRevoked(true);
            deviceCertRepository.save(cert);

            Map<String, String> response = new HashMap<>();
            response.put("message", "기기 인증서가 성공적으로 폐기되었습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
