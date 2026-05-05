package com.example.ca.controller;

import com.example.ca.service.CAService;
import org.bouncycastle.util.encoders.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ca")
public class CAController {

    @Autowired
    private CAService caService;

    @PostMapping("/issue")
    public Map<String, String> issueCertificate(@RequestBody Map<String, String> request) {
        try {
            String deviceId = request.get("deviceId");
            String publicKeyBase64 = request.get("publicKey");

            // Base64 공개키 복원
            byte[] encodedPublicKey = Base64.decode(publicKeyBase64);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA", "BC");
            PublicKey devicePublicKey = keyFactory.generatePublic(new X509EncodedKeySpec(encodedPublicKey));

            // 인증서 발급
            X509Certificate certificate = caService.issueDeviceCertificate(devicePublicKey, deviceId);

            Map<String, String> response = new HashMap<>();
            response.put("certificate", Base64.toBase64String(certificate.getEncoded()));
            response.put("serialNumber", certificate.getSerialNumber().toString());
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return error;
        }
    }

    @PostMapping("/revoke")
    public Map<String, String> revokeCertificate(@RequestBody Map<String, String> request) {
        try {
            BigInteger serialNumber = new BigInteger(request.get("serialNumber"));
            caService.revokeCertificate(serialNumber);
            Map<String, String> response = new HashMap<>();
            response.put("status", "revoked");
            return response;
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return error;
        }
    }

    @GetMapping("/verify/{serialNumber}")
    public Map<String, Object> verifyCertificate(@PathVariable String serialNumber) {
        boolean isRevoked = caService.isRevoked(new BigInteger(serialNumber));
        Map<String, Object> response = new HashMap<>();
        response.put("serialNumber", serialNumber);
        response.put("valid", !isRevoked);
        return response;
    }

    @GetMapping("/root-cert")
    public Map<String, String> getRootCertificate() {
        try {
            X509Certificate rootCert = caService.getRootCertificate();
            Map<String, String> response = new HashMap<>();
            response.put("certificate", Base64.toBase64String(rootCert.getEncoded()));
            return response;
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return error;
        }
    }
}
