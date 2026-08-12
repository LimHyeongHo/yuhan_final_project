package com.Nbbang.backend.domain.member.controller;

import com.Nbbang.backend.domain.auth.entity.DeviceCert;
import com.Nbbang.backend.domain.auth.entity.UserAccount;
import com.Nbbang.backend.domain.auth.repository.DeviceCertRepository;
import com.Nbbang.backend.domain.auth.repository.UserAccountRepository;
import com.Nbbang.backend.domain.auth.service.CAService;
import com.Nbbang.backend.domain.member.service.CertificateSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.spec.X509EncodedKeySpec;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
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

    public MemberInfoController(UserAccountRepository userAccountRepository,
                                 DeviceCertRepository deviceCertRepository,
                                 CertificateSessionService certificateSessionService,
                                 CAService caService) {
        this.userAccountRepository = userAccountRepository;
        this.deviceCertRepository = deviceCertRepository;
        this.certificateSessionService = certificateSessionService;
        this.caService = caService;
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
            body.put("password", user.getPassword());
            body.put("role", user.getRole());
            body.put("createdAt", user.getCreatedAt().format(CREATED_AT_FORMAT));
            deviceCertRepository.findByUserId(userId)
                    .ifPresent(cert -> body.put("certificateSerialNumber", cert.getCertificateSerialNumber()));

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
                user.setPassword(newPassword);
                // user_account뿐 아니라 pki_table(DeviceCert)에도 같은 비밀번호가 중복 저장되어 있어서
                // 여기서 같이 갱신하지 않으면 두 테이블 값이 어긋남.
                deviceCertRepository.findByUserId(userId)
                        .ifPresent(cert -> {
                            cert.setPassword(newPassword);
                            deviceCertRepository.save(cert);
                        });
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

            cert.setDeviceId(deviceId);
            cert.setPublicKey(publicKey);
            cert.setCertificateSerialNumber(serialNumber);
            cert.setRevoked(false);
            deviceCertRepository.save(cert);

            certificateSessionService.startSession(userId); // 10분 유효 타이머 재시작

            Map<String, Object> body = new HashMap<>();
            body.put("certificateSerialNumber", serialNumber);
            body.put("message", "인증서가 재발급되었습니다.");
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    // 회원 탈퇴: CA 인증서 폐기 + 인증서/기기 정보 삭제 + 계정 삭제 + 세션 종료
    @DeleteMapping("/withdraw")
    @Transactional
    public ResponseEntity<Map<String, String>> withdraw(HttpServletRequest request) {
        try {
            String userId = requireUserId(request);

            certificateSessionService.revoke(userId); // CA 폐기 처리 + 인증서 세션 삭제
            deviceCertRepository.findByUserId(userId).ifPresent(deviceCertRepository::delete);
            userAccountRepository.deleteById(userId);

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

    private String requireUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            throw new RuntimeException("로그인이 필요합니다.");
        }
        return (String) session.getAttribute("userId");
    }
}
