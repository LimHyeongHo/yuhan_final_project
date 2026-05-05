package com.springboot.pki.service;

import com.springboot.pki.entity.User;
import com.springboot.pki.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

    private final UserRepository userRepository;
    private final String serverSecret;
    // {userId: challenge}
    private final Map<String, String> challengeStore = new ConcurrentHashMap<>();

    public PkiService(UserRepository userRepository, @Value("${pki.server-secret}") String serverSecret) {
        this.userRepository = userRepository;
        this.serverSecret = serverSecret;
    }

    // 1. CI Hash 생성 (HMAC-SHA256)
    public String generateCiHash(String ci) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(serverSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            return Base64.getEncoder().encodeToString(sha256_HMAC.doFinal(ci.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("CI Hash 생성 중 오류 발생", e);
        }
    }

    // 2. 챌린지 생성
    public String createChallenge(String userId) {
        String challenge = UUID.randomUUID().toString() + ":" + System.currentTimeMillis();
        challengeStore.put(userId, challenge);
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

    public boolean validateChallenge(String userId, String signatureBase64) {
        String originalChallenge = challengeStore.get(userId);
        if (originalChallenge == null) return false;

        User user = userRepository.findByUserId(userId).orElse(null);
        if (user == null || user.getPublicKey() == null) return false;

        boolean isValid = verifySignature(user.getPublicKey(), originalChallenge, signatureBase64);
        if (isValid) {
            challengeStore.remove(userId); // 1회용 사용
        }
        return isValid;
    }
}
