package com.Nbbang.backend.domain.auth.controller;

import org.springframework.web.bind.annotation.*;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.security.*;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test-util")
public class PkiTestUtilController {

    // 1. 테스트용 RSA 키 쌍 생성 (가입할 때 사용)
    @GetMapping("/generate-keys")
    public Map<String, String> generateKeys() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair pair = keyGen.generateKeyPair();

        Map<String, String> keys = new HashMap<>();
        keys.put("publicKey", Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()));
        keys.put("privateKey", Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded()));
        return keys;
    }

    // 2. 챌린지 복호화 (로그인 시 answer 값 생성용)
    //  - challenge: /api/pki/login/challenge 가 내려준 RSA-OAEP(SHA-256) 암호문 (Base64)
    //  - privateKey: 기기 개인키 (PKCS8, Base64)
    //  - 반환된 answer 를 /api/pki/login/verify 에 그대로 넣으면 된다.
    @PostMapping("/decrypt-challenge")
    public Map<String, String> decryptChallenge(@RequestBody Map<String, String> request) {
        String challengeBase64 = request.get("challenge");
        String privateKeyBase64 = request.get("privateKey");

        String answer;
        try {
            byte[] privateBytes = Base64.getDecoder().decode(privateKeyBase64);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PrivateKey privKey = kf.generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(privateBytes));

            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            OAEPParameterSpec oaep = new OAEPParameterSpec(
                    "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);
            cipher.init(Cipher.DECRYPT_MODE, privKey, oaep);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(challengeBase64));
            answer = new String(decrypted, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("챌린지 복호화 실패: " + e.getMessage());
        }

        Map<String, String> result = new HashMap<>();
        result.put("answer", answer);
        return result;
    }
}
