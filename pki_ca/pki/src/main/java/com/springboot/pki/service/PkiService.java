package com.springboot.pki.service;

import com.springboot.pki.entity.DeviceCert;
import com.springboot.pki.repository.DeviceCertRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PkiService {

    @Autowired
    private DeviceCertRepository deviceCertRepository;

    private final String serverSecret;
    private final RestTemplate restTemplate = new RestTemplate();

    // {deviceId: challenge}
    private final Map<String, String> challengeStore = new ConcurrentHashMap<>();

    public PkiService(@Value("${pki.server-secret}") String serverSecret) {
        this.serverSecret = serverSecret;
    }

    // 1. CI Hash 생성 (HMAC-SHA256)
    public String generateCiHash(String ci) {
        if (ci == null || ci.trim().isEmpty()) {
            throw new RuntimeException("CI 값이 비어있습니다. 본인인증을 다시 진행해주세요.");
        }
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(serverSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            return Base64.getEncoder().encodeToString(sha256_HMAC.doFinal(ci.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("CI Hash 생성 중 오류 발생: " + e.getMessage(), e);
        }
    }

    // 2. 챌린지 생성
    public String createChallenge(String deviceId) {
        String challenge = UUID.randomUUID().toString() + ":" + System.currentTimeMillis();
        challengeStore.put(deviceId, challenge);
        return challenge;
    }

    // 3. 서명 검증 (RSA)
    public boolean verifySignature(String publicKeyBase64, String data, String signatureBase64) {
        try {
            byte[] publicBytes = Base64.getDecoder().decode(publicKeyBase64);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey pubKey = keyFactory.generatePublic(keySpec);

            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(pubKey);
            sig.update(data.getBytes(StandardCharsets.UTF_8));
            return sig.verify(Base64.getDecoder().decode(signatureBase64));
        } catch (Exception e) {
            return false;
        }
    }

    public boolean validateChallenge(String deviceId, String signatureBase64) {
        String originalChallenge = challengeStore.get(deviceId);
        if (originalChallenge == null) return false;

        DeviceCert cert = deviceCertRepository.findByDeviceId(deviceId).orElse(null);
        if (cert == null || cert.getPublicKey() == null) return false;

        // 1. DB에서 폐기 여부 확인
        if (cert.isRevoked()) {
            System.out.println("로그인 실패: DB상에서 폐기된 인증서입니다.");
            return false;
        }

        // 2. CA 서버에서 실제 폐기 여부(CRL) 확인
        try {
            Map<String, Object> caStatus = restTemplate.getForObject(
                    "http://localhost:8001/api/ca/verify/" + cert.getCertificateSerialNumber(), Map.class);
            if (caStatus != null && !(Boolean) caStatus.get("valid")) {
                System.out.println("로그인 실패: CA 서버에서 폐기된 인증서입니다.");
                return false;
            }
        } catch (Exception e) {
            System.err.println("CA 서버 검증 중 오류 발생: " + e.getMessage());
        }

        boolean isValid = verifySignature(cert.getPublicKey(), originalChallenge, signatureBase64);
        if (isValid) {
            challengeStore.remove(deviceId);
        }
        return isValid;
    }
}
