package com.Nbbang.backend.domain.auth.service;

import com.Nbbang.backend.domain.auth.entity.DeviceCert;
import com.Nbbang.backend.domain.auth.repository.DeviceCertRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PkiService {

    @Autowired
    private DeviceCertRepository deviceCertRepository;

    @Autowired
    private CAService caService;

    private final String serverSecret;

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

    // 2-1. 챌린지 생성 후 해당 기기 공개키로 암호화하여 반환 (RSA-OAEP)
    //  - 클라이언트는 개인키로 이 값을 복호화해 평문 nonce 를 되돌려줘야 로그인이 성립한다.
    //  - 기기 인증서가 없으면 null (컨트롤러에서 404 처리).
    public String createEncryptedChallenge(String deviceId) {
        DeviceCert cert = deviceCertRepository.findByDeviceId(deviceId).orElse(null);
        if (cert == null || cert.getPublicKey() == null) return null;

        String challenge = createChallenge(deviceId);
        return encryptWithPublicKey(cert.getPublicKey(), challenge);
    }

    // 3. RSA-OAEP(SHA-256) 공개키 암호화
    //  - WebCrypto 의 RSA-OAEP(hash: SHA-256) 와 상호운용하려면 MGF1 해시도 반드시 SHA-256 으로 맞춰야 한다.
    //    (Java 기본값은 MGF1-SHA1 이라 명시하지 않으면 클라이언트에서 복호화 실패)
    public String encryptWithPublicKey(String publicKeyBase64, String plaintext) {
        try {
            byte[] publicBytes = Base64.getDecoder().decode(publicKeyBase64);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey pubKey = keyFactory.generatePublic(keySpec);

            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            OAEPParameterSpec oaep = new OAEPParameterSpec(
                    "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);
            cipher.init(Cipher.ENCRYPT_MODE, pubKey, oaep);
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // 클라이언트가 개인키로 복호화해 되돌려준 평문 nonce 가 서버가 발급한 챌린지와 같은지 검증
    public boolean validateChallenge(String deviceId, String answerPlaintext) {
        String originalChallenge = challengeStore.get(deviceId);
        if (originalChallenge == null) return false;

        DeviceCert cert = deviceCertRepository.findByDeviceId(deviceId).orElse(null);
        if (cert == null || cert.getPublicKey() == null) return false;

        // 1. DB에서 폐기 여부 확인
        if (cert.isRevoked()) {
            System.out.println("로그인 실패: DB상에서 폐기된 인증서입니다.");
            return false;
        }

        // 2. CA 서비스에서 실제 폐기 여부(CRL) 직접 확인
        try {
            boolean isRevoked = caService.isRevoked(new BigInteger(cert.getCertificateSerialNumber()));
            if (isRevoked) {
                System.out.println("로그인 실패: CA 서비스에서 폐기된 인증서입니다.");
                return false;
            }
        } catch (Exception e) {
            System.err.println("CA 서비스 검증 중 오류 발생: " + e.getMessage());
        }

        boolean isValid = originalChallenge.equals(answerPlaintext);
        if (isValid) {
            challengeStore.remove(deviceId);
        }
        return isValid;
    }
}
