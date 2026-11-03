package com.springboot.pki.controller;

import com.springboot.pki.entity.DeviceCert;
import com.springboot.pki.entity.UserAccount;
import com.springboot.pki.repository.DeviceCertRepository;
import com.springboot.pki.repository.UserAccountRepository;
import com.springboot.pki.service.PkiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/pki")
@CrossOrigin(origins = "*")
public class PkiController {

    private final PkiService pkiService;
    private final UserAccountRepository userAccountRepository;
    private final DeviceCertRepository deviceCertRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${portone.api-secret}")
    private String portoneApiSecret;

    @Value("${portone.api.base-url}")
    private String portoneBaseUrl;

    @Value("${ca.server.url:http://localhost:8001/api/ca/issue}")
    private String caServerUrl;

    // 생성자 주입 (권장 방식)
    public PkiController(PkiService pkiService, 
                         UserAccountRepository userAccountRepository, 
                         DeviceCertRepository deviceCertRepository) {
        this.pkiService = pkiService;
        this.userAccountRepository = userAccountRepository;
        this.deviceCertRepository = deviceCertRepository;
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

            // 제네릭 경고 해결을 위해 ParameterizedTypeReference 사용
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

                if (ci == null || ci.trim().isEmpty()) {
                    System.out.println("⚠️ [테스트 모드 감지] CI가 누락되어 임시 CI를 생성합니다.");
                    ci = "TEST_CI_" + java.util.Base64.getEncoder().encodeToString((name + "_" + identityVerificationId).getBytes());
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

            // 1. UserAccount 처리
            UserAccount userAccount = userAccountRepository.findById(email).orElse(new UserAccount());
            userAccount.setEmail(email);
            userAccount.setPassword(password);
            
            if (nickname != null && !nickname.trim().isEmpty()) {
                userAccount.setNickname(nickname);
            }
            if (request.get("role") != null && !request.get("role").trim().isEmpty()) {
                userAccount.setRole(role);
            }
            
            userAccountRepository.saveAndFlush(userAccount);

            // 2. CI Hash & 인증서 발급
            String ciHash = pkiService.generateCiHash(ci);
            Map<String, String> caRequest = new HashMap<>();
            caRequest.put("deviceId", deviceId);
            caRequest.put("publicKey", publicKey);

            // 제네릭 타입 경고 해결
            @SuppressWarnings("unchecked")
            Map<String, Object> caResponse = restTemplate.postForObject(caServerUrl, caRequest, Map.class);

            if (caResponse != null && caResponse.containsKey("certificate")) {
                String serialNumber = String.valueOf(caResponse.get("serialNumber"));

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
            } else {
                throw new RuntimeException("CA 서버로부터 인증서를 발급받지 못했습니다.");
            }
        } catch (Exception e) {
            System.err.println("Registration failed: " + e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
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
public ResponseEntity<Map<String, Object>> verify(@RequestBody Map<String, String> request) {
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
            response.put("success", true);
            response.put("nickname", user.getNickname()); // 닉네임 반환
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

            Map<String, String> caRequest = new HashMap<>();
            caRequest.put("serialNumber", cert.getCertificateSerialNumber());
            restTemplate.postForObject("http://localhost:8001/api/ca/revoke", caRequest, Map.class);

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
