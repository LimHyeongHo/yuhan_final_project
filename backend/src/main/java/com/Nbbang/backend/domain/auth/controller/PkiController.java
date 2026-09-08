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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
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
    private final PasswordEncoder passwordEncoder;

    @Value("${portone.api-secret}")
    private String portoneApiSecret;

    @Value("${portone.api.base-url}")
    private String portoneBaseUrl;

    // 생성자 주입
    public PkiController(PkiService pkiService,
                         UserAccountRepository userAccountRepository,
                         DeviceCertRepository deviceCertRepository,
                         CAService caService,
                         CertificateSessionService certificateSessionService,
                         PasswordEncoder passwordEncoder) {
        this.pkiService = pkiService;
        this.userAccountRepository = userAccountRepository;
        this.deviceCertRepository = deviceCertRepository;
        this.caService = caService;
        this.certificateSessionService = certificateSessionService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 포트원 본인인증 결과 검증
     */
    @PostMapping("/verify-portone")
    public ResponseEntity<?> verifyPortone(@RequestBody Map<String, String> request) {
        String identityVerificationId = request.get("identityVerificationId");
        String email = request.get("email") != null ? request.get("email").replaceAll("\\s", "") : null;

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

                @SuppressWarnings("unchecked")
                Map<String, Object> channel = (Map<String, Object>) response.get("channel");
                boolean isTestChannel = channel != null && "TEST".equals(channel.get("type"));

                // 테스트 채널은 ci 필드가 채워져 오더라도 동일 인물이 다시 인증할 때마다 값이 바뀔 수 있어
                // 계정 재발급/로그인 시 본인 식별 기준으로 쓸 수 없다. 그래서 테스트 채널에서는 PG가 준 ci를
                // 버리고 name+birthDate+phoneNumber로 우리가 직접 안정적인 식별값을 만든다.
                // (라이브 채널로 전환하면 isTestChannel이 false가 되어 PG의 실제 ci를 그대로 신뢰한다.)
                if (isTestChannel || ci == null || ci.trim().isEmpty()) {
                    System.out.println("⚠️ [테스트 채널] name/birthDate/phoneNumber 기반 안정 CI를 생성합니다.");

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

                // 로그인 화면에서 "이 계정 소유자 본인이 인증한 게 맞는지"를 바로 안내할 수 있도록,
                // 이메일이 같이 오고 그 계정에 이미 등록된 기기가 있으면 이번 ci와 저장된 ciHash를 비교해 알려준다.
                if (email != null && !email.isEmpty()) {
                    String candidateCiHash = pkiService.generateCiHash(ci);
                    deviceCertRepository.findByUserId(email).ifPresent(cert ->
                            result.put("matchesAccount", String.valueOf(candidateCiHash.equals(cert.getCiHash()))));
                }

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
    public ResponseEntity<Map<String, Object>> checkEmail(@RequestParam String email) {
        String normalizedEmail = email != null ? email.replaceAll("\\s", "") : "";
        UserAccount existing = normalizedEmail.isEmpty() ? null : userAccountRepository.findById(normalizedEmail).orElse(null);

        Map<String, Object> response = new HashMap<>();
        if (existing == null) {
            response.put("available", true);
            // [MEM-RQ-001] 탈퇴 계정은 영구 재가입 불가 - 프론트가 "이미 사용 중"과 구분해 안내할 수 있도록 사유를 함께 내려준다.
        } else if ("WITHDRAWN".equals(existing.getStatus())) {
            response.put("available", false);
            response.put("reason", "WITHDRAWN");
        } else {
            response.put("available", false);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email") != null ? request.get("email").replaceAll("\\s", "") : null;
            String password = request.get("password");
            String nickname = request.get("nickname");
            // [SEC-RQ-003] 클라이언트가 보낸 role 문자열을 그대로 신뢰하면 안 됨(ROLE_ADMIN 등 임의 값 주입 가능).
            // 서버는 "판매자로 가입하겠다"는 의사표시만 받아들이고, 실제 ROLE_SELLER 승격은
            // AdminController#/users/{email}/grant-seller(관리자 승인)를 통해서만 이뤄지도록 PENDING으로 내린다.
            boolean sellerSignupRequested = "ROLE_SELLER".equals(request.get("role") != null ? request.get("role").trim() : "");
            String role = sellerSignupRequested ? "ROLE_SELLER_PENDING" : "ROLE_BUYER";
            String ci = request.get("ci");
            String publicKey = request.get("publicKey");
            String deviceId = request.get("deviceId") != null ? request.get("deviceId").replaceAll("\\s", "") : email;

            if (email == null || email.isEmpty()) throw new RuntimeException("이메일을 입력해주세요.");

            System.out.println("Processing registration/update for: [" + email + "]");

            // --- 중복 가입 방지 로직 (1인 1계정 정책) ---
            // [MEM-RQ-001] 이 CI로 이미 가입된 계정이 있어도, 그 계정이 탈퇴(WITHDRAWN)한 상태라면
            // 더 이상 "활성 중복 계정"이 아니므로 막지 않는다. 안 그러면 한 번 탈퇴한 사람은
            // 본인인증을 다시 받아도 어떤 이메일로도 영영 재가입할 수 없게 된다.
            String ciHash = pkiService.generateCiHash(ci);
            deviceCertRepository.findByCiHash(ciHash).ifPresent(existingCert -> {
                if (!existingCert.getUserId().equals(email)) {
                    boolean linkedAccountWithdrawn = userAccountRepository.findById(existingCert.getUserId())
                            .map(acc -> "WITHDRAWN".equals(acc.getStatus()))
                            .orElse(false);
                    if (!linkedAccountWithdrawn) {
                        throw new RuntimeException("이미 이 본인인증 정보로 가입된 다른 계정(" + existingCert.getUserId() + ")이 존재합니다.");
                    }
                }
            });
            // ------------------------------------------

            // 1. UserAccount 처리
            UserAccount userAccount = userAccountRepository.findById(email).orElse(null);
            boolean isNewUser = (userAccount == null);

            // [MEM-RQ-001] 탈퇴(WITHDRAWN)한 이메일은 감사 이력 보존을 위해 영구적으로 재가입/기기 재등록 모두 불가.
            // (row 자체는 남아있어 isNewUser=false로 잡히므로, "이미 사용 중" 분기보다 먼저 명확히 구분해서 안내한다.)
            if (!isNewUser && "WITHDRAWN".equals(userAccount.getStatus())) {
                throw new RuntimeException("탈퇴한 계정입니다. 이 이메일로는 다시 가입할 수 없습니다.");
            }

            if (isNewUser) {
                // 신규 가입은 닉네임이 반드시 필요함. 재발급 요청(닉네임 빈 값)이
                // 미가입 이메일로 들어오면 NOT NULL 위반 대신 명확한 에러로 안내.
                if (nickname == null || nickname.trim().isEmpty()) {
                    throw new RuntimeException("가입되지 않은 계정입니다. 회원가입을 먼저 진행해주세요.");
                }
                userAccount = new UserAccount();
                userAccount.setEmail(email);
                userAccount.setPassword(passwordEncoder.encode(password));
                userAccount.setNickname(nickname);
                userAccount.setRole(role); // 서버가 결정한 값(ROLE_BUYER 또는 ROLE_SELLER_PENDING)만 사용
            } else if (nickname != null && !nickname.trim().isEmpty()) {
                // 이미 가입된 이메일로 신규 회원가입(닉네임 포함) 요청이 들어온 경우.
                // 기존에는 여기서 그대로 통과시켜 기존 계정의 비밀번호를 덮어썼음(계정 탈취 가능) -> 거부로 변경.
                throw new RuntimeException("이미 사용 중인 이메일입니다.");
            } else {
                // 재발급/기기 재등록: 닉네임 없이 기존 계정에 대한 요청.
                // 기존 비밀번호와 일치하는지 반드시 확인해야 함 - 확인 없이 통과시키면
                // 이메일만 알아도 아무 비밀번호로 계정을 탈취할 수 있는 심각한 취약점이 됨.
                if (password == null || !passwordEncoder.matches(password, userAccount.getPassword())) {
                    throw new RuntimeException("비밀번호가 일치하지 않습니다.");
                }
                // 검증 용도로만 사용하고, 재발급 과정에서 비밀번호 자체는 변경하지 않음.

                // 비밀번호가 맞아도, 이번 본인인증 결과(ciHash)가 기존 계정에 저장된 본인인증 정보와
                // 다르면 다른 사람의 신원으로 통과한 것이므로 기기 재등록(로그인)을 거부한다.
                deviceCertRepository.findByUserId(email).ifPresent(existing -> {
                    if (existing.getCiHash() != null && !existing.getCiHash().equals(ciHash)) {
                        throw new RuntimeException("본인인증 정보가 기존 계정과 일치하지 않습니다.");
                    }
                });
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
            DeviceCert cert = deviceCertRepository.findByUserId(email).orElse(null);
            if (cert == null) {
                // deviceId는 DB에서 unique라서, 같은 기기로 "새 이메일" 가입 시 그냥 새 행을 insert하면
                // 예전에 그 기기를 쓰던 계정의 행과 충돌한다. 그 기존 계정이 탈퇴(WITHDRAWN) 상태라면
                // 그 행을 새 이메일로 재사용하고, 활성 계정이 여전히 쓰고 있다면 명확히 거부한다.
                DeviceCert existingByDevice = deviceCertRepository.findByDeviceId(deviceId).orElse(null);
                if (existingByDevice != null) {
                    boolean ownerWithdrawnOrGone = userAccountRepository.findById(existingByDevice.getUserId())
                            .map(acc -> "WITHDRAWN".equals(acc.getStatus()))
                            .orElse(true);
                    if (!ownerWithdrawnOrGone) {
                        throw new RuntimeException("이 기기는 이미 다른 계정에 등록되어 있습니다.");
                    }
                    cert = existingByDevice;
                } else {
                    cert = new DeviceCert();
                }
            }

            System.out.println("Updating device cert for: " + email + " -> New Device: " + deviceId);
            
            cert.setUserId(email);
            cert.setDeviceId(deviceId);
            cert.setPublicKey(publicKey);
            cert.setCiHash(ciHash);
            cert.setCertificateSerialNumber(serialNumber);
            cert.setRevoked(false);
            cert.setCertificateIssuedAt(toLocalDateTime(certificate.getNotBefore()));
            cert.setCertificateExpiresAt(toLocalDateTime(certificate.getNotAfter()));
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

            // [MEM-RQ-001] 탈퇴(WITHDRAWN)한 계정은 재로그인 불가
            if ("WITHDRAWN".equals(user.getStatus())) {
                throw new RuntimeException("탈퇴한 계정입니다.");
            }

            if (!passwordEncoder.matches(password, user.getPassword())) {
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
        String normalizedDeviceId = deviceId != null ? deviceId.replaceAll("\\s", "") : "";
        // 기기 공개키로 암호화된 챌린지를 내려준다. 클라이언트가 개인키로 복호화해 되돌려줘야 로그인 성립.
        String encryptedChallenge = pkiService.createEncryptedChallenge(normalizedDeviceId);

        if (encryptedChallenge == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "기기 인증 정보가 없습니다.");
            return ResponseEntity.status(404).body(error);
        }

        Map<String, String> response = new HashMap<>();
        response.put("challenge", encryptedChallenge);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login/verify")
    public ResponseEntity<Map<String, Object>> verify(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        String deviceId = request.get("deviceId") != null ? request.get("deviceId").replaceAll("\\s", "") : "";
        String password = request.get("password");
        // [변경] 서명(signature) 대신, 기기 개인키로 챌린지를 복호화한 평문 nonce
        String answer = request.get("answer");
        String ci = request.get("ci");

        System.out.println("Login verification attempt for device: [" + deviceId + "]");

        try {
            // 1. PKI 서명 검증 및 기기 정보 조회
            DeviceCert cert = deviceCertRepository.findByDeviceId(deviceId)
                    .orElseThrow(() -> new RuntimeException("기기 인증 정보가 없습니다."));

            // 2. 해당 기기와 연결된 사용자 계정 및 비밀번호 확인
            UserAccount user = userAccountRepository.findById(cert.getUserId())
                    .orElseThrow(() -> new RuntimeException("등록되지 않은 사용자입니다."));

            // [MEM-RQ-001] 탈퇴(WITHDRAWN)한 계정은 재로그인 불가
            if ("WITHDRAWN".equals(user.getStatus())) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "탈퇴한 계정입니다.");
                return ResponseEntity.status(403).body(response);
            }

            if (!passwordEncoder.matches(password, user.getPassword())) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "비밀번호가 일치하지 않습니다.");
                return ResponseEntity.status(401).body(response);
            }

            // 2-1. 이번 로그인 시도에서 방금 수행한 본인인증(ci)이 이 기기/계정에 등록된
            // 본인인증 정보와 다르면, 비밀번호와 기기서명이 맞아도 로그인을 거부한다.
            // (그렇지 않으면 본인인증 단계는 isVerified 플래그만 켜는 장식으로 전락해
            //  아무 이름/생년월일/전화번호로 인증해도 로그인이 통과됨)
            if (ci == null || ci.trim().isEmpty() || cert.getCiHash() == null
                    || !cert.getCiHash().equals(pkiService.generateCiHash(ci))) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "본인인증 정보가 계정 소유자와 일치하지 않습니다.");
                return ResponseEntity.status(401).body(response);
            }

            // 3. PKI 챌린지 복호화 결과 검증 (기기 인증)
            boolean isValid = pkiService.validateChallenge(deviceId, answer);

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
                response.put("message", "기기 인증 실패: 폐기된 인증서이거나 챌린지 응답이 올바르지 않습니다.");
                return ResponseEntity.status(401).body(response);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "로그인 오류: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // [SEC-RQ-002] 서버 로그아웃: 현재 HTTP 세션을 무효화한다 (JSESSIONID 쿠키는 세션 무효화 시 자동 만료됨).
    // 수동 로그아웃은 정책상 인증서를 폐기하지 않으므로 caService/certificateSessionService는 호출하지 않는다.
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        Map<String, String> response = new HashMap<>();
        response.put("message", "로그아웃되었습니다.");
        return ResponseEntity.ok(response);
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

    private static LocalDateTime toLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
