package com.springboot.pki.controller;

import org.springframework.web.bind.annotation.*;
import java.security.*;
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

    // 2. 데이터 서명 생성 (로그인/구매 시 signature 값 생성용)
    @PostMapping("/sign")
    public Map<String, String> signData(@RequestBody Map<String, String> request) throws Exception {
        String data = request.get("data");
        String privateKeyBase64 = request.get("privateKey");

        byte[] sigBytes;
        try {
            byte[] privateBytes = Base64.getDecoder().decode(privateKeyBase64);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PrivateKey privKey = kf.generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(privateBytes));

            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(privKey);
            sig.update(data.getBytes("UTF-8"));
            sigBytes = sig.sign();
        } catch (Exception e) {
            throw new RuntimeException("서명 생성 실패: " + e.getMessage());
        }

        Map<String, String> result = new HashMap<>();
        result.put("signature", Base64.getEncoder().encodeToString(sigBytes));
        return result;
    }
}
